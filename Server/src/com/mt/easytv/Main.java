package com.mt.easytv;

import com.frostwire.jlibtorrent.DHT;
import com.frostwire.jlibtorrent.Downloader;
import com.frostwire.jlibtorrent.Session;
import com.mt.easytv.commands.ArgumentNotFoundException;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.commands.CommandNotFoundException;
import com.mt.easytv.config.Config;
import com.mt.easytv.connectivity.Server;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.Torrent;
import com.mt.easytv.torrents.TorrentManager;
import com.mt.easytv.torrents.TorrentState;
import uk.co.caprica.vlcj.component.EmbeddedMediaListPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.embedded.FullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.windows.Win32FullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.x.XFullScreenStrategy;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.version.LibVlcVersion;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main
{
    private static final String CONFIG_PATH = "storage";
    private static final String CONFIG_NAME = "config.properties";
    public static Config config;

    public static Session    torrentSession;
    public static Downloader torrentDownloader;
    public static DHT dhtListener;

    public static TorrentManager torrentManager = new TorrentManager();
    public static CommandHandler commandHandler = new CommandHandler();

    public static JFrame                           displayFrame;
    public static EmbeddedMediaListPlayerComponent mediaPlayer;
    public static Torrent                          playingTorrent;

    private static Server _server;

    public static void main(String[] args) throws Exception {
        Main._checkVlcJInstall();

        Main._initShutdownHook();
        Messager.init();

        /* Config */
        Messager.immediateMessage("Loading config..");
        Main.config = new Config(Main.CONFIG_PATH + "/" + Main.CONFIG_NAME);
        Main._addConfigDefaults();
        Main.config.load();
        Messager.immediateMessage("Config loaded");

        /* Setup JLibtorrent */
        Messager.immediateMessage("Loading JLibTorrent components..");
        Main._initJLibTorrent();
        Messager.immediateMessage("JLibTorrent initialization complete");

        /* Setup vlc window */
        Messager.immediateMessage("Loading VLCJ GUI..");
        Main._initDisplay();
        Messager.immediateMessage("VLCJ ready");

        /* Commands */
        Messager.message("Command handler starting..");
        Main._addCommands();
        Main._processExecCommands(args);
        Main.commandHandler.addReader(new BufferedReader(new InputStreamReader(System.in)));
        Messager.message("Command handler ready..");

        /* Client listener */
        Main._server = new Server();
        Main._server.startListening();

        int sleepTime = Integer.parseInt(Main.config.getValue("sleepTime"));
        Messager.startRedrawing(Integer.parseInt(Main.config.getValue("redrawTime")));

        /* Process commands, etc */
        while (true) {
            try {
                Main.commandHandler.read(); //if there are commands then these are blocking, this is so we don't overflow the pi's memory doing multiple searches async
                Main._server.checkWaitingClients();
                Main._server.processAllCommands(Main.commandHandler);
                Thread.sleep(sleepTime); //to stop it redrawing so much
            }
            catch (ArgumentNotFoundException e) {
                Messager.message(e.getMessage());
            }
            catch (CommandNotFoundException e) {
                Messager.message(e.getMessage());
            }
            catch (Exception e) {
                Messager.error("Command reading error: ", e);
            }
        }
    }

    private static void _checkVlcJInstall() throws Exception {
        if (!RuntimeUtil.isWindows() && !RuntimeUtil.isNix()) {
            throw new Exception("Your operating system is not supported.");
        }

        NativeDiscovery discoverer = new NativeDiscovery();
        discoverer.discover();

        // discovery()'s method return value is WRONG on Linux
        try {
            LibVlcVersion.getVersion();
        }
        catch (Exception e) {
            throw new Exception("VLC install not found.");
        }
    }

    private static void _initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run() {
                Messager.immediateMessage("Shutting down..");

                /* Update config to save changes to files */
                try {
                    Messager.immediateMessage("Saving config");
                    Main.config.save();
                    Messager.immediateMessage("Saved config");
                }
                catch (Exception e) {
                    Messager.error("Failed to save config ", e);
                }

                /* Stop command handler */
                try {
                    Main.commandHandler.clearReaders();
                }
                catch (Exception e) {
                    Messager.error("Failed to close listeners: ", e);
                }

                /* Stop the server listener */
                try {
                    Main._server.stopListening();
                }
                catch (Exception e) {
                    Messager.error("Failed to stop listener server ", e);
                }

                /* Save resume data */
                for (Torrent torrent : Main.torrentManager.getDownloadingTorrents()) {
                    if (torrent.getState() == TorrentState.DOWNLOADING) {
                        torrent.getDownload().getHandle().saveResumeData();
                    }
                }

                /* Save the current state of the downloader */
                try {
                    Messager.immediateMessage("Saving torrent downloader state");

                    byte[] stateBytes = Main.torrentSession.saveState();
                    Files.write(Main._getSaveStatePath(), stateBytes);

                    Messager.immediateMessage("Torrent downloader state saved");
                }
                catch (IOException e) {
                    Messager.error("Failed to save torrent downloader state", e);
                }

                /* Stop the downloader */
                Messager.immediateMessage("Shutting down torrent downloader");
                Main.torrentSession.stopDHT();
                Main.torrentSession.pause();
                Messager.immediateMessage("Torrent downloader shutdown");
                Messager.stopRedrawing();
            }
        });
    }

    private static void _initJLibTorrent() {
        Main.torrentSession = new Session();
        Main.torrentDownloader = new Downloader(Main.torrentSession);
        Main.dhtListener = new DHT(Main.torrentSession);

        /* Setup the sync manager that maintains contact between the server and clients and handles errors */
        Main.torrentSession.addListener(new TorrentSyncManager());

        /* Start the DHT server for fetching magnetdata from hash */
        Messager.immediateMessage("DHT waiting for nodes");
        Main.dhtListener.waitNodes(1);
        Messager.immediateMessage("DHT found " + Main.dhtListener.nodes() + " nodes");

        /* Load the previous state of the downloader */
        Path statePath = Main._getSaveStatePath();

        if (statePath.toFile().exists()) {
            Messager.immediateMessage("Attempting to read downloader saved state.");

            try {
                Main.torrentSession.loadState(Files.readAllBytes(statePath));
                Messager.immediateMessage("Successfully read downloader saved state.");
            }
            catch (Exception e) {
                Messager.error("Error reading downloader state", e);
            }
        }
    }

    private static void _addConfigDefaults() {
        /* System */
        Main.config.addDefault("sleepTime", "1000");
        Main.config.addDefault("redrawTime", "250");

        /* Server */
        Main.config.addDefault("port", "8080");
        Main.config.addDefault("expectedMagic", "fireyourlaz0r");

        /* Storage */
        Main.config.addDefault("storage", "storage");
        Main.config.addDefault("torrentCache", "torrents");
        Main.config.addDefault("torrentFiles", "torrentsData");
        Main.config.addDefault("torrentResume", "torrentsInProgress");
        Main.config.addDefault("session", "downloadState.session");

        /* TPB local */
        Main.config.addDefault("tpbIndex", "storage/tpbindex.txt");

        /* Searching options */
        Main.config.addDefault("idSize", "30");
        Main.config.addDefault("matchThreshold", "60");
        Main.config.addDefault("defaultTorrentListSize", "10");

        /* Downloading options */
        Main.config.addDefault("magnetTimeout", "30000");
        Main.config.addDefault("seedAfterDownload", "false");
        Main.config.addDefault("downloadLimit", 1024 * 1024 * 1024); //1024MB, because why not
        Main.config.addDefault("uploadLimit", (1024 * 1024 * 1024) / 4); //250MB
    }

    private static void _initDisplay() {
        Main.displayFrame = new JFrame();
        Main.displayFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        Main.displayFrame.setLocation(0, 0);
        Main.displayFrame.setSize(Toolkit.getDefaultToolkit().getScreenSize());

        Main.mediaPlayer = new EmbeddedMediaListPlayerComponent()
        {
            @Override
            protected FullScreenStrategy onGetFullScreenStrategy() {
                return RuntimeUtil.isWindows() ? new Win32FullScreenStrategy(Main.displayFrame) : new XFullScreenStrategy(Main.displayFrame);
            }
        };

        Main.displayFrame.setContentPane(Main.mediaPlayer);
    }

    private static void _addCommands() {
        Main.commandHandler.addCommand("quit", CLICommands::quit);
        Main.commandHandler.addCommand("getConfig", CLICommands::getConfig);
        Main.commandHandler.addCommand("setConfig", CLICommands::setConfig);
        Main.commandHandler.addCommand("search", CLICommands::search, false);
        Main.commandHandler.addCommand("sort", CLICommands::sort, false);
        Main.commandHandler.addCommand("view", CLICommands::view);
        Main.commandHandler.addCommand("viewOne", CLICommands::viewOne);
        Main.commandHandler.addCommand("download", CLICommands::download);
        Main.commandHandler.addCommand("stopDownload", CLICommands::stopDownload);
        Main.commandHandler.addCommand("play", CLICommands::play);
        Main.commandHandler.addCommand("delete", CLICommands::delete);
        Main.commandHandler.addCommand("unstickAll", CLICommands::unstickAll);
    }

    private static void _processExecCommands(String[] args) throws Exception {
        for (String arg : args) {
            Main.commandHandler.processCommand(arg.startsWith("-") ? arg.replaceFirst("-", "") : arg);
        }
    }

    private static Path _getSaveStatePath() {
        return Paths.get(Main.config.concatValues(new String[]{"storage", "session"}, "//"));
    }
}

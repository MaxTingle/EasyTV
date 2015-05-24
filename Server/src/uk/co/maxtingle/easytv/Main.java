package uk.co.maxtingle.easytv;

import com.frostwire.jlibtorrent.DHT;
import com.frostwire.jlibtorrent.Downloader;
import com.frostwire.jlibtorrent.Session;
import uk.co.caprica.vlcj.component.EmbeddedMediaListPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.embedded.FullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.windows.Win32FullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.x.XFullScreenStrategy;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.version.LibVlcVersion;
import uk.co.maxtingle.communication.common.AuthState;
import uk.co.maxtingle.communication.debug.Debugger;
import uk.co.maxtingle.communication.server.Server;
import uk.co.maxtingle.easytv.commands.ArgumentNotFoundException;
import uk.co.maxtingle.easytv.commands.CommandHandler;
import uk.co.maxtingle.easytv.commands.CommandNotFoundException;
import uk.co.maxtingle.easytv.config.Config;
import uk.co.maxtingle.easytv.interaction.Messager;
import uk.co.maxtingle.easytv.torrents.Torrent;
import uk.co.maxtingle.easytv.torrents.TorrentManager;
import uk.co.maxtingle.easytv.torrents.TorrentState;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main
{
    private static final String   LIB_JAR_PATH = "/";
    private static final String[] LIB_EXT      = {"so", "dll"};
    private static final String[] LIBS         = {"jlibtorrent"};

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
    public static Server server;

    private static boolean _running = true;

    public static void main(String[] args) throws Exception {
        if (!RuntimeUtil.isWindows() && !RuntimeUtil.isNix()) {
            throw new Exception("Your operating system is not supported.");
        }

        /* Load libs and startup core */
        Main._exportLibs();
        Main._checkVlcJInstall();
        Main._checkJlibtorrentInstall();

        Main._initShutdownHook();

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

        /* Cleanup threads */
        Messager.message("Starting TorrentManager cleanup threads..");
        Main.torrentManager.startCleaner();
        TorrentManager.startSearchCleaner();
        Messager.message("TorrentManager cleanup threads started.");

        /* Setup the messager */
        int sleepTime = Integer.parseInt(Main.config.getValue("sleepTime"));
        Messager.startRedrawing(Integer.parseInt(Main.config.getValue("redrawTime")));

        /* Client listener */
        Debugger.setLogger((String category, String message) -> {
            if (_running) {
                Messager.message("[" + category + "] " + message);
            }
            else {
                Messager.immediateMessage("[" + category + "] " + message);
            }
        });

        Main.server = new Server();
        Main.server.onMessageReceived(Main.commandHandler::processCommand);
        Main.server.onClientAuthStateChanged((authState, newState, baseClient) -> { //inform our client about all the notable torrents
            if(newState == AuthState.ACCEPTED) {
                Main.torrentManager.getTorrents().forEach(torrent -> {
                    if (torrent.getState() != TorrentState.LOADED && torrent.getState() != TorrentState.SEARCHED) {
                        Messager.informClientAboutTorrent(baseClient, torrent);
                    }
                });
                Debugger.log("App", "Informed new client about current torrents");
            }
        });

        try {
            Main.server.start();
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        /* Process commands, etc */
        while (Main._running) {
            try {
                Main.commandHandler.read(); //if there are commands then these are blocking, this is so we don't overflow the pi's memory doing multiple searches async
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

        Messager.immediateMessage("Main stopped listening for commands");
    }

    private static void _exportLibs() throws IOException {
        if (new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isDirectory()) {
            return; //running from ide or other environment, not a jar.
        }

        for (int libIndex = 0; libIndex < Main.LIBS.length; libIndex++) {
            for (int extIndex = 0; extIndex < Main.LIB_EXT.length; extIndex++) {
                String path = Main.LIBS[libIndex] + "." + Main.LIB_EXT[extIndex];

                File exportFile = new File(path);
                exportFile.deleteOnExit();
                if (exportFile.exists()) {
                    continue;
                }
                else if (!exportFile.createNewFile()) {
                    throw new FileAlreadyExistsException("Failed to create export file " + path);
                }

                InputStream resourceStream = Main.class.getResourceAsStream(Main.LIB_JAR_PATH + path);
                if (resourceStream == null) {
                    throw new FileNotFoundException("Library " + Main.LIBS[libIndex] + " failed to load. Not found.");
                }

                FileOutputStream exportStream = new FileOutputStream(exportFile);

                byte[] resourceBuffer = new byte[1024];
                int resourceBytes;

                while ((resourceBytes = resourceStream.read(resourceBuffer)) != -1) {
                    exportStream.write(resourceBuffer, 0, resourceBytes);
                }
            }
        }
    }

    private static void _checkJlibtorrentInstall() throws Exception {
        try {
            Class.forName("com.frostwire.jlibtorrent.swig.libtorrent_jni");
        }
        catch (UnsatisfiedLinkError e) {
            throw new Exception("JLibtorrent install not found. (" + e.getMessage() + ")");
        }
    }

    private static void _checkVlcJInstall() throws Exception {
        NativeDiscovery discoverer = new NativeDiscovery();
        discoverer.discover();

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

                try {
                    Main._running = false;
                    Thread.sleep(Integer.parseInt(Main.config.getValue("sleepTime"))); //wait until main has stopped
                }
                catch (Exception e) {
                    Messager.error("Failed to shutdown main", e);
                }

                /* Stop the torrent manage cleaners */
                Messager.immediateMessage("Stopping TorrentManager cleaner services...");
                Main.torrentManager.stopCleaner();
                TorrentManager.stopSearchCleaner();
                Messager.immediateMessage("Cleaner services stopped");

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
                    Main.server.stop();
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

                /* Load in torrents */
                Main.torrentManager.loadFromSession(Main.torrentSession);
                Main.torrentSession.resume();
                Messager.immediateMessage("Successfully read in all torrens");
            }
            catch (Exception e) {
                Messager.error("Error reading downloader state", e);
            }
        }
    }

    private static void _addConfigDefaults() {
        /* System */
        Main.config.addDefault("sleepTime", 1000);
        Main.config.addDefault("redrawTime", 1000);
        Main.config.addDefault("cleanInterval", 1000 * 60 * 60 * 3); //every 3 hours

        /* Server */
        Main.config.addDefault("port", "8080");
//        Main.config.addDefault("username", "test");
//        Main.config.addDefault("password", "test");
//        Main.config.addDefault("serverMagic", "fYrEUrl4z0r");
//        Main.config.addDefault("clientMagic", "alLIlLlR1ightYth4n");

        /* Storage */
        Main.config.addDefault("storage", "storage");
        Main.config.addDefault("torrentCache", "torrents");
        Main.config.addDefault("torrentFiles", "torrentsData");
        Main.config.addDefault("torrentResume", "torrentsInProgress");
        Main.config.addDefault("session", "downloadState.session");
        Main.config.addDefault("logLocation", "server.log");

        /* TPB local */
        Main.config.addDefault("tpbIndex", "tpbindex.txt");
        Main.config.addDefault("tpbThreads", 4);

        /* Searching options */
        Main.config.addDefault("idSize", 30);
        Main.config.addDefault("matchThreshold", 60);
        Main.config.addDefault("minimumSeeders", 5);
        Main.config.addDefault("defaultTorrentListSize", 10);

        /* Downloading options */
        Main.config.addDefault("magnetTimeout", 30000);
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
        /* CLI commands */
        Main.commandHandler.addCommand("clear", CLICommands::clear);
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
        Main.commandHandler.addCommand("clearTorrents", CLICommands::clearTorrents);

        /* Client commands */
        Main.commandHandler.addCommand("getCommands", ClientCommands::getCommands);
        Main.commandHandler.addCommand("search", ClientCommands::search, false);
        Main.commandHandler.addCommand("cancelDownload", ClientCommands::cancelDownload, false);
        Main.commandHandler.addCommand("delete", ClientCommands::delete, false);
        Main.commandHandler.addCommand("download", ClientCommands::download, false);
        Main.commandHandler.addCommand("play", ClientCommands::play, false);
        Main.commandHandler.addCommand("pause", ClientCommands::pause, false);
        Main.commandHandler.addCommand("get", ClientCommands::get, false);
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

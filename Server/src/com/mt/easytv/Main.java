package com.mt.easytv;

import com.frostwire.jlibtorrent.DHT;
import com.frostwire.jlibtorrent.Downloader;
import com.frostwire.jlibtorrent.Session;
import com.mt.easytv.commands.ArgumentNotFoundException;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.commands.CommandNotFoundException;
import com.mt.easytv.config.Config;
import com.mt.easytv.connection.Server;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.TorrentManager;

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
    public static Session    torrentSession;
    public static Downloader torrentDownloader;
    public static DHT        dhtServer;
    public static CommandHandler commandHandler = new CommandHandler();
    public static TorrentManager torrentManager = new TorrentManager();
    public static Config config;
    private static Server _server;

    public static void main(String[] args) throws Exception {
        Main._initShutdownHook();

        /* Setup core */
            /* config */
        Main.config = new Config(Main.CONFIG_PATH + "/" + Main.CONFIG_NAME);
        Main._addConfigDefaults();
        Main.config.load();

            /* Commands */
        Main.commandHandler.start(); //start its thread, on its own thread so it can listen
        Main._addCommands();
        Main._processExecCommands(args);
        Main.commandHandler.addReader(new BufferedReader(new InputStreamReader(System.in)));

        /* Setup JLibtorrent */
        Messager.message("Initializing JLibTorrent components");
        Main._initJLibTorrent();
        Messager.message("JLibTorrent initialization complete");

        /* Create listener */
        Main._server = new Server();
        Main._server.startListening();

        int sleepTime = Integer.parseInt(Main.config.getValue("sleepTime")); //experimental to try free up the cpu some of the time

        /* Process commands, etc */
        while (true) {
            try {
                Main.commandHandler.read();
                Main._server.checkWaitingClients();
                Main._server.processAllCommands(Main.commandHandler);
                Thread.sleep(sleepTime);
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

    private static void _initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run() {
                Messager.message("Shutting down..");

                /* Update config to save changes to files */
                try {
                    Messager.message("Saving config");
                    Main.config.save();
                    Messager.message("Saved config");
                }
                catch (Exception e) {
                    Messager.error("Failed to save config ", e);
                }

                /* Stop command handler */
                try {
                    Main.commandHandler.clearListeners();
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

                /* Save the current state of the downloader */
                try {
                    Messager.message("Saving torrent downloader state");

                    byte[] stateBytes = Main.torrentSession.saveState();
                    Path statePath = Paths.get(Main.config.concatValues(new String[]{"storage", "session"}));
                    Files.write(statePath, stateBytes);

                    Messager.message("Torrent downloader state saved");
                }
                catch (IOException e) {
                    Messager.error("Failed to save torrent downloader state", e);
                }

                /* Stop the downloader */
                Messager.message("Shutting down torrent downloader");
                Main.torrentSession.stopDHT();
                Main.torrentSession.pause();
                Messager.message("Torrent downloader shutdown");
            }
        });
    }

    private static void _initJLibTorrent() {
        Main.torrentSession = new Session();
        Main.torrentDownloader = new Downloader(Main.torrentSession);
        Main.dhtServer = new DHT(Main.torrentSession);

        /* Start the DHT server for fetching magnetdata from hash */
        Messager.message("DHT waiting for nodes");
        Main.dhtServer.waitNodes(1);
        Messager.message("DHT found " + Main.dhtServer.nodes() + " nodes");

        /* Load the previous state of the downloader */
        Path statePath = Paths.get(Main.config.concatValues(new String[]{"storage", "session"}));

        if (statePath.toFile().exists()) {
            Messager.message("Attempting to read downloader saved state.");

            try {
                Main.torrentSession.loadState(Files.readAllBytes(statePath));
                Messager.message("Successfully read downloader saved state.");
            }
            catch (Exception e) {
                Messager.error("Error reading downloader state", e);
            }
        }
    }

    private static void _addConfigDefaults() {
        /* System */
        Main.config.addDefault("sleepTime", "1000");

        /* Server */
        Main.config.addDefault("port", "8080");
        Main.config.addDefault("expectedMagic", "fireyourlaz0r");

        /* Storage */
        Main.config.addDefault("storage", "storage");
        Main.config.addDefault("torrentCache", "torrents");
        Main.config.addDefault("torrentFiles", "torrentsData");
        Main.config.addDefault("torrentResume", "torrentsInProgress");
        Main.config.addDefault("session", "torrentsession.session");

        /* TPB local */
        Main.config.addDefault("tpbIndex", "tpb index.txt");

        /* Searching options */
        Main.config.addDefault("idSize", "30");
        Main.config.addDefault("matchThreshold", "60");
        Main.config.addDefault("defaultTorrentListSize", "10");

        /* Downloading options */
        Main.config.addDefault("magnetTimeout", "30000");
        Main.config.addDefault("downloadLimit", "1024");
        Main.config.addDefault("uploadLimit", "100");
    }

    private static void _addCommands() {
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "quit", CLICommands::quit);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "getConfig", CLICommands::getConfig);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "setConfig", CLICommands::setConfig);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "search", CLICommands::search, false);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "sort", CLICommands::sort, false);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "view", CLICommands::view);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "viewOne", CLICommands::viewOne);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "download", CLICommands::download);
    }

    private static void _processExecCommands(String[] args) throws Exception {
        for (String arg : args) {
            Main.commandHandler.processCommand(arg.startsWith("-") ? arg.replaceFirst("-", "") : arg);
        }
    }
}

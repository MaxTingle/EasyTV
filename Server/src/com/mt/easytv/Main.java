package com.mt.easytv;

import com.mt.easytv.commands.ArgumentNotFoundException;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.commands.CommandNotFoundException;
import com.mt.easytv.config.Config;
import com.mt.easytv.connection.Server;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.TorrentLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Main
{
    private static final String CONFIG_PATH = "tmp/";
    private static final String CONFIG_NAME = "config.properties";
    public static Config config;
    public static CommandHandler commandHandler = new CommandHandler();
    public static TorrentLoader  torrentLoader  = new TorrentLoader();
    private static Server _server;


    public static void main(String[] args) throws Exception {
        Main._initShutdownHook();

        /* Setup core */
            /* config */
        Main.config = new Config(Main.CONFIG_PATH + Main.CONFIG_NAME);
        Main._addConfigDefaults();
        Main.config.load();

            /* Commands */
        Main._addCommands();
        Main._processExecCommands(args);
        Main.commandHandler.addReader(new BufferedReader(new InputStreamReader(System.in)));

        /* Create listener */
        Main._server = new Server();
        Main._server.start();
        Main._server.startListening();

        /* Process commands, etc */
        while (true) {
            try {
                Main.commandHandler.read();
                Main._server.checkWaitingClients();
                Main._server.processAllCommands(Main.commandHandler);
            }
            catch (ArgumentNotFoundException e) {
                Messager.message(e.getMessage());
            }
            catch (CommandNotFoundException e) {
                Messager.message(e.getMessage());
            }
            catch (Exception e) {
                Messager.error("Command reading error", e);
            }
        }
    }

    private static void _initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run() {
                try {
                    Main.config.save();
                    Main._server.stopListening();
                }
                catch (IOException e) {
                    Messager.message("Failed to save config " + e.getMessage());
                }

                Main.commandHandler.destroy();
            }
        });
    }

    private static void _addConfigDefaults() {
        /* Server */
        Main.config.addDefault("port", "8080");
        Main.config.addDefault("expectedMagic", "fireyourlaz0r");

        /* Downloading */
        Main.config.addDefault("tmpRoot", "tmp");
        Main.config.addDefault("tmpTorrents", "torrents");

        /* TPB local */
        Main.config.addDefault("tpbIndex", "tpb index.txt");

        /* Searching options */
        Main.config.addDefault("matchThreshold", "60");
        Main.config.addDefault("defaultTorrentListSize", "10");
    }

    private static void _addCommands() {
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "quit", CLICommands::quit);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "getConfig", CLICommands::getConfig);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "setConfig", CLICommands::setConfig);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "search", CLICommands::search);
        Main.commandHandler.addCommand(CommandHandler.CommandSource.CLI, "viewTorrents", CLICommands::viewTorrents);
    }

    private static void _processExecCommands(String[] args) throws Exception {
        for (String arg : args) {
            Main.commandHandler.processCommand(arg.startsWith("-") ? arg.replaceFirst("-", "") : arg);
        }
    }
}

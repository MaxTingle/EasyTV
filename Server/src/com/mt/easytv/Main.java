package com.mt.easytv;

import com.mt.easytv.commands.ArgumentNotFoundException;
import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.commands.CommandNotFoundException;
import com.mt.easytv.config.Config;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.Torrent;
import com.mt.easytv.torrents.TorrentLoader;
import com.mt.easytv.torrents.sources.TorrentSourceNotFound;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Main
{
    private static final String CONFIG_PATH = "tmp/";
    private static final String CONFIG_NAME = "config.properties";
    public static Config config;
    private static CommandHandler _commandHandler = new CommandHandler();
    private static TorrentLoader  _torrentLoader  = new TorrentLoader();

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
        Main._commandHandler.addReader(new BufferedReader(new InputStreamReader(System.in)));

        /* Process commands, etc */
        while (true) {
            try {
                Main._commandHandler.read();
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

    public static void quit() //dummy function in-case extra functionality needed later
    {
        System.exit(0);
    }

    private static void _initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run() {
                try {
                    Main.config.save();
                }
                catch (IOException e) {
                    Messager.message("Failed to save config " + e.getMessage());
                }

                Main._commandHandler.destroy();
            }
        });
    }

    private static void _addConfigDefaults() {
        Main.config.addDefault("tmpRoot", "tmp");
        Main.config.addDefault("tmpTorrents", "torrents");
        Main.config.addDefault("tpbIndex", "tpb index.txt");
        Main.config.addDefault("matchThreshold", "60");
        Main.config.addDefault("defaultTorrentListSize", "10");
    }

    private static void _addCommands() {
        /* Quit */
        Main._commandHandler.addCommand("quit", (CommandArgument[] args) -> Main.quit());

        /*
          Displays all the config item values
          getValue
            * required:
                __itemName__     **No key, should just the key of the config item
        */
        Main._commandHandler.addCommand("getConfig", (CommandArgument[] args) -> {
            if (args.length < 1) {
                Messager.message("getConfig requires the config name");
                return;
            }

            for (CommandArgument arg : args) {
                String value = Main.config.getValue(arg.value);

                if (value == null) {
                    Messager.message("Config item " + arg.value + " does not exist.");
                }
                else {
                    Messager.message(arg.value + " = " + value);
                }
            }
        });

        /*
          Sets item values in the config, specify as many config items as you want
          setConfig
            * required:
                -__configItem__     **Key should be the name of the config item, value the value to set it to
        */
        Main._commandHandler.addCommand("setConfig", (CommandArgument[] args) -> {
            if (args.length < 1) {
                Messager.message("setConfig requires the config name and value. (-name \"value\")");
                return;
            }

            for (CommandArgument arg : args) {
                if (arg.value == null) {
                    Messager.message("config item " + arg.argument + " not updated, value not given.");
                    continue;
                }

                Main.config.setValue(arg.argument, arg.value);
            }

            try {
                Main.config.save(); //just in-case use the debugger close button which kills jvm completely
                Messager.message("Config updated.");
            }
            catch (IOException e) {
                Messager.message("Config update failed " + e.getMessage());
            }
        });

        /*
          Torrent searching / loading
          viewTorrents
            * required:
                -search   "search terms"                     **The keywords to search for
                -searchIn [piratebay,piratebaylocal,kickass] **The sources of torrents to look in
            * optional:
                -view     **Displays the first 10 torrents after loading
                -progress **Shows progress as loading
        */
        Main._commandHandler.addCommand("search", (CommandArgument[] args) -> {
            CommandArgument searchIn = null;
            CommandArgument searchFor = null;
            boolean view = false;
            boolean progress = false;

            for (CommandArgument arg : args) {
                if (arg.argument.equals("searchIn")) {
                    searchIn = arg;
                }
                else if (arg.argument.equals("search")) {
                    searchFor = arg;
                }
                else if (arg.argument.equals("view")) {
                    view = true;
                }
                else if (arg.argument.equals("progress")) {
                    progress = true;
                }
            }

            if (searchIn == null || searchFor == null) {
                Messager.message("search requires the what to search for(-search \"to search for\") and where to search(-searchin \"piratebay, piratebaylocal, kickass\").");
                return;
            }

            Messager.message("Searching..");

            try {
                int loadedCount = Main._torrentLoader.load(searchFor.value, searchIn.value.split(","), progress);
                Messager.message("Loaded " + loadedCount + " torrents");

                if (view) {
                    Main._commandHandler.processCommand("viewTorrents");
                }
            }
            catch (TorrentSourceNotFound e) {
                Messager.message(e.getMessage());
            }
            catch (Exception e) {
                Messager.error("Error searching torrents: ", e);
            }
        });

        /*
          Viewing torrents
          viewTorrents
            * optional:
                -size 10 **Specifies the number of torrents to load
                -reset   **Resets the current index to 0
        */
        Main._commandHandler.addCommand("viewTorrents", (CommandArgument[] args) -> {
            if (!Main._torrentLoader.hasTorrents()) {
                Messager.message("No torrents loaded");
                return;
            }

            int pageSize = Integer.parseInt(Main.config.getValue("defaultTorrentListSize"));

            for (CommandArgument arg : args) {
                if (arg.argument == null) {
                    pageSize = Integer.parseInt(arg.value);
                }
                else if (arg.argument.equals("size")) {
                    pageSize = Integer.parseInt(arg.value);
                }
                else if (arg.argument.equals("reset")) {
                    Main._torrentLoader.resetPageIndex();
                }
            }

            Torrent[] torrents = Main._torrentLoader.next(pageSize);

            for (Torrent torrent : torrents) {
                Messager.message(torrent.toString());
            }
        });
    }

    private static void _processExecCommands(String[] args) throws Exception {
        for (String arg : args) {
            Main._commandHandler.processCommand(arg.startsWith("-") ? arg.replaceFirst("-", "") : arg);
        }
    }
}

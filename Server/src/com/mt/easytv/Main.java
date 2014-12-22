package com.mt.easytv;

import com.mt.easytv.commands.ArgumentNotFoundException;
import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.commands.CommandNotFoundException;
import com.mt.easytv.config.Config;
import com.mt.easytv.torrents.Torrent;
import com.mt.easytv.torrents.TorrentLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public final class Main
{
    private static final String CONFIG_PATH = "tmp/";
    private static final String CONFIG_NAME = "config.properties";
    public static Config config;
    private static CommandHandler _commandHandler;

    public static void main(String[] args) throws Exception
    {
        Main._initShutdownHook();

        /* Setup core */
            /* config */
        Main.config = new Config(Main.CONFIG_PATH + Main.CONFIG_NAME);
        Main._addConfigDefaults();
        Main.config.load();

            /* Commands */
        Main._commandHandler = new CommandHandler();
        Main._addCommands();
        Main._processExecCommands(args);
        Main._commandHandler.addReader(new BufferedReader(new InputStreamReader(System.in)));

        /* Process commands, etc */
        while (true) {
            try {
                Main._commandHandler.read();
            } catch (ArgumentNotFoundException e) {
                Main.message(e.getMessage());
            } catch (CommandNotFoundException e) {
                Main.message(e.getMessage());
            } catch (Exception e) {
                Main.error("Command reading error", e);
            }
        }
    }

    public static void quit() //dummy function in-case extra functionality needed later
    {
        System.exit(0);
    }

    public static void message(String error) //dummy function in-case extra functionality needed later
    {
        System.out.println(error);
    }

    public static void error(String prefix, Exception e)
    {
        Main.message(prefix + e.getMessage());

        for (StackTraceElement stack : e.getStackTrace()) {
            Main.message("in " + stack.getFileName() + "." + stack.getClassName() + "." + stack.getMethodName() + " on line " + stack.getLineNumber());
        }
    }

    private static void _initShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                try {
                    Main.config.save();
                } catch (IOException e) {
                    Main.message("Failed to save config " + e.getMessage());
                }

                Main._commandHandler.destroy();
            }
        });
    }

    private static void _addConfigDefaults()
    {
        Main.config.addDefault("tmpRoot", "tmp");
        Main.config.addDefault("tmpTorrents", "torrents");
        Main.config.addDefault("tpbIndex", "tpb index.txt");
        Main.config.addDefault("matchThreshold", "60");
    }

    private static void _addCommands()
    {
        /* Quit */
        Main._commandHandler.addCommand("quit", (CommandArgument[] args) -> Main.quit());

        /* config */

        /* get */
        Main._commandHandler.addCommand("getConfig", (CommandArgument[] args) -> {
            if (args.length < 1) {
                Main.message("getConfig requires the config name");
                return;
            }

            for (CommandArgument arg : args) {
                String value = Main.config.getValue(arg.value);

                if (value == null) {
                    Main.message("Config item " + arg.value + " does not exist.");
                } else {
                    Main.message(arg.value + " = " + value);
                }
            }
        });

        /* set */
        Main._commandHandler.addCommand("setConfig", (CommandArgument[] args) -> {
            if (args.length < 1) {
                Main.message("setConfig requires the config name and value. (-name \"value\")");
                return;
            }

            for (CommandArgument arg : args) {
                if (arg.value == null) {
                    Main.message("config item " + arg.argument + " not updated, value not given.");
                    continue;
                }

                Main.config.setValue(arg.argument, arg.value);
            }

            try {
                Main.config.save(); //just in-case use the debugger close button which kills jvm completely
                Main.message("Config updated.");
            } catch (IOException e) {
                Main.message("Config update failed " + e.getMessage());
            }
        });

        /* Torrent searching server side */
        Main._commandHandler.addCommand("search", (CommandArgument[] args) -> {
            CommandArgument searchIn = null;
            CommandArgument searchFor = null;
            for (CommandArgument arg : args) {
                if (arg.argument.equals("searchIn")) {
                    searchIn = arg;
                } else if (arg.argument.equals("search")) {
                    searchFor = arg;
                }
            }

            if (searchIn == null || searchFor == null) {
                Main.message("search requires the what to search for(-search \"to search for\") and where to search(-searchin \"piratebay, piratebaylocal, kickass\").");
                return;
            }

            try {
                ArrayList<Torrent> torrents = TorrentLoader.search(searchFor.value, searchIn.value.split(","));

                for (Torrent torrent : torrents) {
                    Main.message(torrent.toString());
                }
            } catch (Exception e) {
                Main.error("Error searching torrents: ", e);
            }
        });
    }

    private static void _processExecCommands(String[] args) throws Exception
    {
        for (String arg : args) {
            Main._commandHandler.processCommand(arg.startsWith("-") ? arg.replaceFirst("-", "") : arg);
        }
    }
}

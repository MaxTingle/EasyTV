package com.mt.easytv;

import com.mt.easytv.commands.ArgumentNotFoundException;
import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.commands.CommandNotFoundException;
import com.mt.easytv.config.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Main
{
    private static final String CONFIG_PATH = "tmp/";
    private static final String CONFIG_NAME = "config.properties";
    public static  Config         Config;
    private static CommandHandler _commandHandler;

    public static void main(String[] args) throws Exception
    {
        Main._initShutdownHook();

        /* Setup core */
            /* Config */
        Main.Config = new Config(Main.CONFIG_PATH + Main.CONFIG_NAME);
        Main._addConfigDefaults();
        Main.Config.load();

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

    private static void _initShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                try {
                    Main.Config.save();
                } catch (IOException e) {
                    Main.message("Failed to save config " + e.getMessage());
                }

                Main._commandHandler.destroy();
            }
        });
    }

    private static void _addConfigDefaults()
    {
        Main.Config.addDefault("tmpRoot", "tmp");
        Main.Config.addDefault("tmpTorrents", "%1$stmp");
        Main.Config.addDefault("torrentSearch", "https://oldpiratebay.org/search.php?q=%1$s}");
    }

    private static void _addCommands()
    {
        /* Quit */
        Main._commandHandler.addCommand("quit", (CommandArgument[] args) -> Main.quit());

        /* Config */
        Main._commandHandler.addCommand("setConfig", (CommandArgument[] args) -> {
            if (args.length < 1) {
                Main.message("setConfig requires the config name and value.");
                return;
            }

            Main.Config.setValue(args[0].argument, args[0].value);
            System.out.println("Config updated.");
        });
    }

    private static void _processExecCommands(String[] args) throws Exception
    {
        for (String arg : args) {
            if (arg.startsWith("-")) {
                Main._commandHandler.processCommand(arg.replace("-", ""));
            }
        }
    }
}

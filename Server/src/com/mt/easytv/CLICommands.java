package com.mt.easytv;

import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.Torrent;
import com.mt.easytv.torrents.sources.TorrentSourceNotFound;

import java.io.IOException;

public class CLICommands
{
    /**
     * Displays all the config item values
     * required:
     * __itemName__     **No key, should just the key of the config item
     */
    public static void getConfig(CommandArgument[] args) {
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
    }

    /**
     * Sets item values in the config, specify as many config items as you want
     * required:
     * -__configItem__     **Key should be the name of the config item, value the value to set it to
     */
    public static void setConfig(CommandArgument[] args) {
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
    }

    public static void quit(CommandArgument[] args) {
        System.exit(0);
    }

    /**
     * Torrent searching / loading
     * required:
     * -search   "search terms"                     **The keywords to search for
     * -searchIn [piratebay,piratebaylocal,kickass] **The sources of torrents to look in
     * optional:
     * -view     **Displays the first 10 torrents after loading
     * -progress **Shows progress as loading
     */
    public static void search(CommandArgument[] args) {
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
            int loadedCount = Main.torrentLoader.load(searchFor.value, searchIn.value.split(","), progress);
            Messager.message("Loaded " + loadedCount + " torrents");

            if (view) {
                Main.commandHandler.processCommand("viewTorrents");
            }
        }
        catch (TorrentSourceNotFound e) {
            Messager.message(e.getMessage());
        }
        catch (Exception e) {
            Messager.error("Error searching torrents: ", e);
        }
    }

    /**
     * Viewing torrents
     * optional:
     * -size 10 **Specifies the number of torrents to load
     * -reset   **Resets the current index to 0
     */
    public static void viewTorrents(CommandArgument[] args) {
        if (!Main.torrentLoader.hasTorrents()) {
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
                Main.torrentLoader.resetPageIndex();
            }
        }

        Torrent[] torrents = Main.torrentLoader.next(pageSize);

        for (Torrent torrent : torrents) {
            Messager.message(torrent.toString());
        }
    }
}
package com.mt.easytv;

import com.frostwire.jlibtorrent.TorrentInfo;
import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.Torrent;
import com.mt.easytv.torrents.TorrentDownload;
import com.mt.easytv.torrents.TorrentState;
import com.mt.easytv.torrents.sources.TorrentSourceNotFound;

import java.io.File;
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
     * -view     **Displays n torrents. May pass the number to view or none in which case will use default
     * -progress **Shows progress as loading
     * -sortBy   **What to sort by, must be desc
     */
    public static void search(CommandArgument[] args) {
        CommandArgument searchIn = null;
        CommandArgument searchFor = null;

        boolean progress = false;
        int view = 0;
        String sortBy = null;

        for (CommandArgument arg : args) {
            if (arg.argument.equals("searchIn")) {
                searchIn = arg;
            }
            else if (arg.argument.equals("search")) {
                searchFor = arg;
            }
            else if (arg.argument.equals("view")) {
                if (arg.value != null) {
                    view = Integer.parseInt(arg.value);
                }
                else {
                    view = Integer.parseInt(Main.config.getValue("defaultTorrentListSize"));
                }
            }
            else if (arg.argument.equals("progress")) {
                progress = true;
            }
            else if (arg.argument.equals("sortBy")) {
                sortBy = arg.value;
            }
        }

        if (searchIn == null || searchFor == null) {
            Messager.message("search requires the what to search for(-search \"to search for\") and where to search(-searchin \"piratebay, piratebaylocal, kickass\").");
            return;
        }

        Messager.message("Searching..");

        try {
            int loadedCount = Main.torrentManager.load(searchFor.value, searchIn.value.split(","), progress);
            Messager.message("Loaded " + loadedCount + " torrents");

            if (sortBy != null) {
                Main.commandHandler.processCommand("sort -sortBy " + sortBy);
            }
            if (view > 0) {
                Main.commandHandler.processCommand("view " + view);
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
     * Outputs a certain amount of the torrent's detail
     * optional:
     * -size    **Specifies the number of torrents to load
     * -reset   **Resets the current index to 0
     */
    public static void view(CommandArgument[] args) {
        if (!Main.torrentManager.hasTorrents()) {
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
                Main.torrentManager.resetPageIndex();
            }
        }

        Torrent[] torrents = Main.torrentManager.next(pageSize);

        for (Torrent torrent : torrents) {
            Messager.message(torrent.toString());
        }
    }

    /**
     * Sorts the torrent manager torrents by a user specified variable
     * -sortBy    **What to sort by [seeders,leechers,size]
     * -sortDir   **What direction to sort [asc,desc]
     */
    public static void sort(CommandArgument[] args) {
        String sortBy = null;
        String sortDir = "desc";

        for (CommandArgument arg : args) {
            if (arg.argument.equals("sortBy")) { //== as argument can be null
                sortBy = arg.value;
            }
            else if (arg.argument.equals("sortDir")) {
                sortDir = arg.value;
            }
        }

        if (sortBy == null) {
            Messager.message("Sort requires the sortBy argument (sort -sortBy {sortMode} or sort {sortMode})");
            return;
        }

        try {
            Main.torrentManager.sort(sortBy, sortDir);
            Main.torrentManager.resetPageIndex();
            Messager.message("Sort complete");
        }
        catch (Exception e) {
            Messager.message(e.getMessage());
        }
    }

    /**
     * Outputs a single torrent's detail
     * -id    **The ID of the torrent to view
     */
    public static void viewOne(CommandArgument[] args) {
        Torrent torrent = CLICommands._torrentFromArgs(args, true);

        if (torrent == null) {
            Messager.message("Torrent not found.");
        }
        else {
            Messager.message(torrent.toString());
        }
    }

    /**
     * Deletes a torrent's data
     * -id    **The ID of the torrent to delete
     */
    public static void delete(CommandArgument[] args) throws Exception {
        Torrent torrent = CLICommands._torrentFromArgs(args, true);

        if (torrent == null) {
            Messager.message("Torrent not found.");
        }
        else if (torrent.getDownload().deleteFiles()) {
            Messager.message("Torrent files deleted.");
        }
        else {
            Messager.message("Torrent files failed to delete.");
        }
    }

    /**
     * Starts a torrent downlaod
     * -id      **The ID of the torrent to download
     * -force   **Whether or not to force redownload
     */
    public static void download(CommandArgument[] args) throws Exception {
        Torrent torrent = CLICommands._torrentFromArgs(args, true);
        boolean force = false;

        for (CommandArgument arg : args) {
            if (arg.argument != null && arg.argument.equals("force")) {
                force = true;
                break;
            }
        }

        if (torrent == null) {
            return;
        }
        else if (torrent.getState() == null) {
            torrent.loadState();
        }

        if (!force && torrent.getState() == TorrentState.DOWNLOADED) {
            Messager.message("Torrent is already downloaded, add -force to redownload.");
            return;
        }
        else if (torrent.getState() == TorrentState.ACTIONED) {
            Messager.message("Torrent is in playback state.");
            return;
        }

        if (torrent.getState() == TorrentState.DOWNLOADED) {
            TorrentDownload download = torrent.getDownload();
            TorrentInfo torrentInfo = download.getTorrentInfo();

            if (torrentInfo == null) {
                Messager.message("Torrent info not found.");
                return;
            }

            int totalFiles = torrentInfo.getFiles().geNumFiles();

            for (int i = 0; i < totalFiles; i++) {
                File file = new File(torrentInfo.getFiles().getFilePath(i));

                if (!file.delete()) {
                    Messager.message("Failed to delete existing file " + file.getName());
                    return;
                }
            }
        }

        torrent.getDownload().download((int percentCompleted) -> {
            String percentStr = torrent.getState() == TorrentState.DOWNLOADING ? percentCompleted + "%" : "";
            Messager.message("Downloading: " + torrent.name + " download at " + torrent.getState() + " " + percentStr);
        });
    }

    private static Torrent _torrentFromArgs(CommandArgument[] args, boolean allowEmpty) {
        String id = null;

        for (CommandArgument arg : args) {
            if (arg.argument == null) {
                if (allowEmpty) { //sub if so else if doesn't need not null check
                    id = arg.value; //so can just do {command} adfsdfsdf
                }
            }
            else if (arg.argument.equals("id")) {
                id = arg.value;
            }
        }

        if (id == null) {
            Messager.message("The torrent id is required. (viewOne -id {id} or viewOne {id})");
            return null;
        }

        Torrent torrent = Main.torrentManager.get(id);

        if (torrent == null) {
            Messager.message("Torrent not found.");
        }

        return torrent;
    }
}
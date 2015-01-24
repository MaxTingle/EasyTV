package com.mt.easytv;

import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.commands.CommandArgumentList;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.interaction.PersistentMessage;
import com.mt.easytv.torrents.Torrent;
import com.mt.easytv.torrents.TorrentState;
import com.mt.easytv.torrents.sources.TorrentSourceNotFound;

import java.io.IOException;

public class CLICommands
{
    /**
     * Displays all the config item values
     * required:
     * __itemName__     **No key, should just the key of the config item
     */
    public static void getConfig(CommandArgumentList args) {
        if (args.size() < 1) {
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
    public static void setConfig(CommandArgumentList args) {
        if (args.size() < 1) {
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

    public static void quit(CommandArgumentList args) {
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
    public static void search(CommandArgumentList args) {
        String searchIn = args.getValue("searchIn");
        String searchFor = args.getValue("search");

        boolean progress = args.getValue("progress", null) != null;
        int view = Integer.parseInt(args.getValue("view", Main.config.getValue("defaultTorrentListSize")));
        String sortBy = args.getValue("sortBy");

        if (searchIn == null || searchFor == null) {
            Messager.message("search requires the what to search for(-search \"to search for\") and where to search(-searchin \"piratebay, piratebaylocal, kickass\").");
            return;
        }

        Messager.message("Searching..");

        try {
            int loadedCount = Main.torrentManager.load(searchFor, searchIn.split(","), progress);
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
    public static void view(CommandArgumentList args) {
        if (!Main.torrentManager.hasTorrents()) {
            Messager.message("No torrents loaded");
            return;
        }

        if (args.getValue("reset", null) != null) {
            Main.torrentManager.resetPageIndex();
        }

        int pageSize = Integer.parseInt((String) args.getValue("size", Main.config.getValue("defaultTorrentListSize"), true));
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
    public static void sort(CommandArgumentList args) {
        String sortBy = args.getValue("sortBy");
        String sortDir = args.getValue("sortDir", "desc");

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
     * -id     **The ID of the torrent to view
     * -sticky **Whether or not to stick the torrent view
     * -unsticky **Whether or not to unstick the torrent view
     */
    public static void viewOne(CommandArgumentList args) {
        boolean makeUnsticky = args.getValue("sticky", null) != null;
        boolean makeSticky = args.getValue("unsticky", null) != null;

        if (makeSticky && makeUnsticky) {
            Messager.message("Cannot unstick and stick torrent at the same time!");
            return;
        }

        Torrent torrent = CLICommands._torrentFromArgs(args, true);

        if (torrent == null) {
            return;
        }

        if (makeSticky) {
            PersistentMessage persistentMessage = Messager.getPersistentMessage(torrent);

            if (persistentMessage != null) {
                Messager.message("Cannot stick torrent, torrent is already stickied.");
                return;
            }

            Messager.addPersistentMessage((String previous) -> torrent.toString(), torrent);
        }
        else if (makeUnsticky) {
            PersistentMessage persistentMessage = Messager.getPersistentMessage(torrent);

            if (persistentMessage == null) {
                Messager.message("Cannot unstick torrent, torrent not stickied.");
                return;
            }

            if (!Messager.removePersistentMessage(persistentMessage)) {
                Messager.message("Failed to remove persistent message.");
            }
        }
        else {
            Messager.message(torrent.toString());
        }
    }

    /**
     * Deletes a torrent's data
     * -id    **The ID of the torrent to delete
     */
    public static void delete(CommandArgumentList args) throws Exception {
        Torrent torrent = CLICommands._torrentFromArgs(args, true);

        if (torrent == null) {
            return;
        }
        else if (torrent.getDownload().isDownloading() && !torrent.getDownload().stopDownload()) {
            Messager.message("Torrent failed to pause.");
            return;
        }

        if (torrent.getDownload().deleteFiles()) {
            Messager.message("Torrent files deleted.");
        }
        else {
            Messager.message("Torrent files failed to delete.");
        }
    }

    /**
     * Stops a torrent downloading
     * -id    **The ID of the torrent to delete
     */
    public static void stopDownload(CommandArgumentList args) throws Exception {
        Torrent torrent = CLICommands._torrentFromArgs(args, true);

        if (torrent == null) {
            return;
        }
        else if (!torrent.getDownload().isDownloading()) {
            Messager.message("Torrent not downloading.");
            return;
        }

        if (torrent.getDownload().stopDownload()) {
            Messager.message("Download stopped");
        }
        else {
            Messager.message("Download failed to stop.");
        }
    }

    /**
     * Plays a torrent file using vlc
     * -id    **The ID of the torrent to delete
     * -file  **The file to play if there are multiple
     */
    public static void play(CommandArgumentList args) throws Exception {
        Torrent torrent = CLICommands._torrentFromArgs(args, true);
        String fileToPlay = args.getValue("file");

        if (torrent == null) {
            return;
        }
        else if (fileToPlay == null) {
            Messager.message("Please specify the torrent file you want to play.");
            return;
        }
        else if (torrent.getState() != TorrentState.DOWNLOADED) {
            Messager.message("Torrent not downloaded.");
            return;
        }

        torrent.play(fileToPlay);
    }

    /**
     * Starts a torrent download
     * -id      **The ID of the torrent to download
     * -force   **Whether or not to force redownload
     */
    public static void download(CommandArgumentList args) throws Exception {
        Torrent torrent = CLICommands._torrentFromArgs(args, true);
        boolean force = args.getValue("force", null) != null;

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

        if (torrent.getState() == TorrentState.DOWNLOADED) { //&& force
            torrent.getDownload().deleteFiles();
        }

        PersistentMessage message = Messager.addPersistentMessage((String previousMessage) ->
                                                                          "Downloading " + torrent.id + ": " + torrent.name + " download at " + torrent.getState() + " " +
                                                                          (torrent.getState() == TorrentState.DOWNLOADING ? torrent.getDownload().getPercentDownloaded() + "%" : "")
        );

        torrent.getDownload().download((int percent) -> {
            if (torrent.getState() == TorrentState.DOWNLOADED) {
                Messager.message("Downloading " + torrent.id + ": " + torrent.name + " completed");
                Messager.removePersistentMessage(message);
            }
            else if (percent == -1) {//download cancelled
                Messager.message("Downloading " + torrent.id + ": " + torrent.name + " cancelled");
                Messager.removePersistentMessage(message);
            }
        });
    }

    /**
     * Unsticks all stuck messages
     */
    public static void unstickAll(CommandArgumentList args) {
        Messager.removeAllPersistentMessages();
    }

    private static Torrent _torrentFromArgs(CommandArgumentList args, boolean allowEmpty) {
        String id = args.getValue("id", allowEmpty);

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
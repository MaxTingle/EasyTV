package com.mt.easytv;


import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.commands.CommandArgumentList;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.torrents.Torrent;
import com.mt.easytv.torrents.TorrentManager;
import com.mt.easytv.torrents.TorrentState;
import uk.co.maxtingle.communication.debug.Debugger;
import uk.co.maxtingle.communication.server.ServerClient;

import java.util.ArrayList;

public class ClientCommands
{
    /**
     * Gets all the ClientCommands that a client can run
     */
    public static Object[] getCommands(CommandArgumentList args, ServerClient client) {
        return Main.commandHandler.getCommands(CommandHandler.CommandSource.CLIENT);
    }

    /**
     * Torrent searching / loading
     *
     * required:
     * -search   "search terms"                     **The keywords to search for
     * -searchIn [piratebay,piratebaylocal,kickass] **The sources of torrents to look in
     */
    public static Object[] search(CommandArgumentList args, ServerClient client) throws Exception {
        CommandArgument searchIn = args.get("searchIn");
        String searchFor = args.getValue("search");

        if (searchIn == null) {
            throw new Exception("Search in must be an array of strings");
        }
        else if (searchFor == null) {
            throw new Exception("Please enter a search term and search area.");
        }

        String[] searchInArr;

        if (searchIn.value instanceof ArrayList) {
            ArrayList searchInList = (ArrayList) searchIn.value;
            searchInArr = (String[]) searchInList.toArray(new String[searchInList.size()]);
        }
        else {
            searchInArr = (String[]) searchIn.value;
        }

        String searchInStr = "";
        for (String str : searchInArr) {
            searchInStr += searchInStr.equals("") ? str : ", " + str;
        }

        Debugger.log("App", "Searching for " + searchFor + " in " + searchInStr);
        return TorrentManager.search(searchFor, searchInArr).toArray();
    }

    /**
     * Downloads a torrent from the loaded torrents
     *
     * required:
     * -id
     */
    public static Object[] download(CommandArgumentList args, ServerClient client) throws Exception {
        ClientCommands._getTorrent(args, false).getDownload().download();
        return null;
    }

    /**
     * Cancels a downloading, loaded torrent
     *
     * required:
     * -id
     */
    public static Object[] cancelDownload(CommandArgumentList args, ServerClient client) throws Exception {
        Torrent torrent = ClientCommands._getTorrent(args, true);

        if(torrent.getState() != TorrentState.DOWNLOADING && torrent.getState() != TorrentState.DOWNLOADING_META) {
            throw new Exception("Cannot cancel a torrent download that is not downloading.");
        }
        else if(!torrent.getDownload().stopDownload()) {
            throw new Exception("Failed to stop download.");
        }

        return null;
    }

    /**
     * Plays a torrent from the loaded torrents
     *
     * required:
     * -id
     */
    public static Object[] play(CommandArgumentList args, ServerClient client) throws Exception {
        ClientCommands._getTorrent(args, true).play(args.getValue("file"));
        return null;
    }

    /**
     * Pauses a playing, loaded torrent
     *
     * required:
     * -id
     */
    public static Object[] pause(CommandArgumentList args, ServerClient client) throws Exception {
        Torrent torrent = ClientCommands._getTorrent(args, true);

        if(torrent.getState() != TorrentState.ACTIONED) {
            throw new Exception("Cannot pause a torrent that is not playing.");
        }

        torrent.pause();
        return null;
    }

    private static Torrent _getTorrent(CommandArgumentList args, boolean alreadyLoaded) throws Exception {
        String id = args.getValue("id", false);

        if (id == null) {
            throw new Exception("ID passed as null.");
        }

        Torrent torrent = Main.torrentManager.get(id);

        if (!alreadyLoaded && torrent == null) {
            Main.torrentManager.loadSearchedTorrent(id);
            torrent = Main.torrentManager.get(id);
        }

        if (torrent == null) {
            throw new Exception("Torrent not found.");
        }

        return torrent;
    }
}
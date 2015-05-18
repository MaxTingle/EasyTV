package com.mt.easytv;


import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.commands.CommandArgumentList;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.TorrentManager;
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

        Messager.message("Searching for " + searchFor + " in " + searchInStr);
        return TorrentManager.search(searchFor, searchInArr).toArray();
    }
}
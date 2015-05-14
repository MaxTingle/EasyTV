package com.mt.easytv;


import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.commands.CommandArgumentList;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.TorrentManager;
import uk.co.maxtingle.communication.server.ServerClient;

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

        if (searchIn == null || !(searchIn.value instanceof String[])) {
            throw new Exception("Search in must be an array of strings");
        }
        else if (searchFor == null) {
            throw new Exception("Please enter a search term and search area.");
        }

        Messager.message("Searching for " + searchIn + " in " + searchFor);
        return TorrentManager.search(searchFor, (String[]) searchIn.value).toArray();
    }
}
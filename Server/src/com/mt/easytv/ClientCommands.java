package com.mt.easytv;


import com.mt.easytv.commands.CommandArgumentList;
import com.mt.easytv.commands.CommandHandler;
import uk.co.maxtingle.communication.server.ServerClient;

public class ClientCommands
{
    public static Object[] getCommands(CommandArgumentList command, ServerClient client) {
        return Main.commandHandler.getCommands(CommandHandler.CommandSource.CLIENT);
    }
}
package com.mt.easytv.commands;

import com.mt.easytv.connection.Client;

public class Command implements ICommand, IClientCommand
{
    public String command = "";
    public boolean allowNullArgument;
    public CommandHandler.CommandSource source;

    public void processCommand(CommandArgument[] args) throws Exception {
        throw new Exception("Not implemented");
    }

    public Object processCommand(CommandArgument[] args, Client client) throws Exception {
        throw new Exception("Not implemented");
    }
}
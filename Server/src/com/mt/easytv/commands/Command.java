package com.mt.easytv.commands;

import com.mt.easytv.connectivity.Client;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Command implements ICommand, IClientCommand
{
    /**
     * The command name
     */
    public String command = "";

    /**
     * Whether or not to allow arguments in the CommandArgumentList array
     * passed to the processCommand function to have the value not null
     * and the argument null
     */
    public boolean allowNullArgument;

    /**
     * Where this command can be executed from
     */
    public CommandHandler.CommandSource source;

    public void processCommand(CommandArgumentList args) throws Exception {
        throw new NotImplementedException();
    }

    public Object[] processCommand(CommandArgumentList args, Client client) throws Exception {
        throw new NotImplementedException();
    }
}
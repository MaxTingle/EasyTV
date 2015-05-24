package uk.co.maxtingle.easytv.commands;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import uk.co.maxtingle.communication.server.ServerClient;

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

    public Object[] processCommand(CommandArgumentList args, ServerClient client) throws Exception {
        throw new NotImplementedException();
    }
}
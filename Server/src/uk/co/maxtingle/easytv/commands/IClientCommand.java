package uk.co.maxtingle.easytv.commands;

import uk.co.maxtingle.communication.server.ServerClient;

@FunctionalInterface
/**
 * The methods needed for executing a command
 * coming from a client
 *
 * @see com.maxtingle.easytv.connectivity.Client
 */
public interface IClientCommand
{
    /**
     * Executes a command coming from an authenticated client
     *
     * @param args The array of arguments the client passed
     * @param client The server client that made the request
     * @return The result that might normally be printed to screen, so it can be added to a
     * message and passed back to the client
     */
    Object[] processCommand(CommandArgumentList args, ServerClient client) throws Exception;
}
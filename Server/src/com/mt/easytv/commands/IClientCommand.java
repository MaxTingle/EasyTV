package com.mt.easytv.commands;

import uk.co.maxtingle.communication.client.Client;

@FunctionalInterface
/**
 * The methods needed for executing a command
 * coming from a client
 *
 * @see com.mt.easytv.connectivity.Client
 */
public interface IClientCommand
{
    /**
     * Executes a command coming from an authenticated client
     *
     * @param args The array of arguments the client passed
     * @return The result that might normally be printed to screen, so it can be added to a
     * message and passed back to the client
     */
    Object[] processCommand(CommandArgumentList args, Client client) throws Exception;
}
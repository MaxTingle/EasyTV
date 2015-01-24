package com.mt.easytv.commands;

@FunctionalInterface
/**
 * The methods needed for executing a command
 * coming from the CLI controlling the application
 */
public interface ICommand
{
    /**
     * Executes a command coming from the CLI associated with the application
     *
     * @param args The array of arguments the command was executed with
     */
    void processCommand(CommandArgumentList args) throws Exception;
}
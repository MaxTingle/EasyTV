package com.mt.easytv.commands;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import uk.co.maxtingle.communication.common.BaseClient;
import uk.co.maxtingle.communication.common.Message;
import uk.co.maxtingle.communication.server.ServerClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The facade that handles all aspects of management
 * (Creating, processing, reading)
 * all types of commands. (Server client and CLI)
 */
public final class CommandHandler
{
    private ArrayList<Command>        _commands = new ArrayList<>();
    private ArrayList<BufferedReader> _readers  = new ArrayList<>();

    public enum CommandSource
    {
        ANY,
        CLIENT,
        CLI
    }

    /**
     * Gets an array of the added commands
     *
     * @param source The source to get the commands for or null for any
     * @return The commands
     */
    public String[] getCommands(@Nullable final CommandSource source) {
        return this._commands.stream().filter(command -> command.source == source || source == null)
                             .map(command -> command.command)
                             .toArray(String[]::new);
    }

    /**
     * Adds the command to the list of registered and
     * processable commands.
     *
     * @param command The command to register and listen for
     */
    public void addCommand(@NotNull Command command) {
        this._commands.add(command);
    }

    /**
     * Creates a command from the details given and adds it
     * to the list of processable commands. Will execute
     * the action method inside the command's implementation
     * of said method.
     * Command can only be executed by CLI sources as it will
     * only implement void processCommand
     * Command will allow null arguments
     *
     * @param commandName The name of the command that is checked
     *                    against when processCommand is called
     * @param action      The implementation of ICommand that has
     *                    the processCommand method to execute
     * @return The created command
     */
    public Command addCommand(@NotNull String commandName, @NotNull ICommand action) {
        return this.addCommand(CommandSource.CLI, commandName, action, null, true);
    }

    /**
     * Creates a command from the details given and adds it
     * to the list of processable commands. Will execute
     * the action method inside the command's implementation
     * of said method.
     * Command can only be executed by CLI sources as it will
     * only implement void processCommand
     *
     * @param commandName       The name of the command that is checked
     *                          against when processCommand is called
     * @param action            The implementation of ICommand that has
     *                          the processCommand method to execute
     * @param allowNullArgument Whether or not to allow any
     *                          CommandArgument.arguments in the
     *                          args array processCommand call is
     *                          it with to be null
     * @return The created command
     */
    public Command addCommand(@NotNull String commandName, @NotNull ICommand action, boolean allowNullArgument) {
        return this.addCommand(CommandSource.CLI, commandName, action, null, allowNullArgument);
    }

    /**
     * Creates a command from the details given and adds it
     * to the list of processable commands. Will execute
     * the action method inside the command's implementation
     * of said method.
     * Command can only be executed by Client sources as it will
     * only implement Object processCommand
     * Command will allow null arguments
     *
     * @param commandName The name of the command that is checked
     *                    against when processCommand is called
     * @param action      The implementation of IClientCommand that has
     *                    the processCommand method to execute
     * @return The created command
     */
    public Command addCommand(@NotNull String commandName, @NotNull IClientCommand action) {
        return this.addCommand(CommandSource.CLIENT, commandName, null, action, true);
    }

    /**
     * Creates a command from the details given and adds it
     * to the list of processable commands. Will execute
     * the action method inside the command's implementation
     * of said method.
     * Command can only be executed by Client sources as it will
     * only implement Object processCommand
     *
     * @param commandName       The name of the command that is checked
     *                          against when processCommand is called
     * @param action            The implementation of IClientCommand that has
     *                          the processCommand method to execute
     * @param allowNullArgument Whether or not to allow any
     *                          CommandArgument.arguments in the
     *                          args array processCommand call is
     *                          it with to be null
     * @return The created command
     */
    public Command addCommand(@NotNull String commandName, @NotNull IClientCommand action, boolean allowNullArgument) {
        return this.addCommand(CommandSource.CLIENT, commandName, null, action, allowNullArgument);
    }

    /**
     * Creates a command from the details given and
     * adds it to the list of processable commands
     *
     * @param source            Which sources the command can be ran on
     *                          (All sources allowed should have their
     *                          associated interfaces not null)
     * @param commandName       The name of the command that is checked
     *                          against when processCommand is called
     * @param cliAction         The implementation of ICommand that has
     *                          the processCommand method to execute whenever
     *                          a CLI sourced command comes in
     * @param clientAction      The implementation of IClientCommand that has
     *                          the processCommand method to execute whenever
     *                          a CLI sourced command comes in
     * @param allowNullArgument Whether or not to allow any
     *                          CommandArgument.arguments in the
     *                          args array processCommand call is
     *                          it with to be null
     * @return The created command
     */
    public Command addCommand(@NotNull CommandSource source, @NotNull String commandName, @Nullable ICommand cliAction,
                              @Nullable IClientCommand clientAction, boolean allowNullArgument
    ) {
        Command command = new Command()
        {
            @Override
            public void processCommand(CommandArgumentList args) throws Exception {
                if (cliAction == null) {
                    throw new NotImplementedException();
                }

                cliAction.processCommand(args);
            }

            @Override
            public Object[] processCommand(CommandArgumentList args, ServerClient client) throws Exception {
                if (clientAction == null) {
                    throw new NotImplementedException();
                }

                return clientAction.processCommand(args, client);
            }
        };
        command.source = source;
        command.command = commandName;
        command.allowNullArgument = allowNullArgument;

        this.addCommand(command);
        return command;
    }

    /**
     * Removes the first command that has the
     * command value equal to the commandName
     *
     * @param commandName The command to search for
     * @return Whether or not the command was found
     * and removed
     */
    public boolean removeCommand(@NotNull String commandName) {
        for (Command command : this._commands) {
            if (commandName.equals(command.command)) {
                this.removeCommand(command);
                return true;
            }
        }

        return false;
    }

    /**
     * Removes the command from the list of
     * processable commands, meaning it can
     * no longer be used
     *
     * @param command The command to remove
     * @return Whether or not the command removed
     */
    public boolean removeCommand(@NotNull Command command) {
        return this._commands.remove(command);
    }

    /**
     * Gets the command and command arguments
     * from a string and attempts to find
     * and execute said command.
     * Only executes CLI or ANY based commands.
     *
     * @param commandFull The command and arguments
     *                    EG: quit -force yes
     * @throws java.lang.Exception                              Exception thrown by the command
     * @throws com.mt.easytv.commands.CommandNotFoundException  Failed to find the command to execute
     * @throws com.mt.easytv.commands.ArgumentNotFoundException Argument failed parsing or is not allowed value
     */
    public void processCommand(@NotNull String commandFull) throws Exception {
        /* Building up argument array */
        String[] commandParts = commandFull.split(" ");

        if (commandParts.length < 1) {
            throw new CommandNotFoundException(commandFull);
        }

        String[] argumentParts = new String[commandParts.length - 1];
        System.arraycopy(commandParts, 1, argumentParts, 0, commandParts.length - 1);

        CommandArgumentList args = CommandArgument.fromArray(argumentParts);
        Command command = this._findCommand(commandParts[0], args, false);

        command.processCommand(args);
    }

    /**
     * Gets the command and command arguments
     * from a string and attempts to find
     * and execute said command
     * Only executes CLIENT or ANY based commands.
     *
     * @param clientMessage The command from the client
     * @param client        The client that wants to execute the given command
     * @throws java.lang.Exception                              Exception thrown by the command
     * @throws com.mt.easytv.commands.CommandNotFoundException  Failed to find the command to execute
     * @throws com.mt.easytv.commands.ArgumentNotFoundException Argument failed parsing or is not allowed value
     */
    public void processCommand(@NotNull BaseClient client, @NotNull Message clientMessage) throws Exception {
        CommandArgumentList args = CommandArgumentList.fromMessage(clientMessage);

        if (args == null) {
            clientMessage.respond(new Message(false, "Invalid arguments"));
        }
        else {
            Command command = this._findCommand(clientMessage.request, args, true);

            try {
                clientMessage.respond(new Message(true, command.processCommand(args, (ServerClient) client)));
            }
            catch (Exception e) {
                clientMessage.respond(new Message(false, e.getMessage()));
            }
        }
    }

    /**
     * Adds a BufferedReader to the list of
     * sources for commands.
     *
     * @param reader The reader to add to the list
     */
    public void addReader(@NotNull BufferedReader reader) {
        this._readers.add(reader);
    }

    /**
     * Removes a BufferedReader from the list of
     * sources for commands.
     *
     * @param reader The reader to remove from the list
     * @return Whether or not the reader was found and removed
     */
    public boolean removeReader(@NotNull BufferedReader reader) {
        return this._readers.remove(reader);
    }

    /**
     * Reads from all the BufferedReaders in
     * the list of sources for commands. Then
     * processes any commands that have come
     * through from the BufferedReaders.
     * Not blocking
     *
     * @throws java.lang.Exception                              Exception thrown by the command
     * @throws com.mt.easytv.commands.CommandNotFoundException  Failed to find the command to execute
     * @throws com.mt.easytv.commands.ArgumentNotFoundException Argument failed parsing or is not allowed value
     */
    public void read() throws Exception {
        for (BufferedReader reader : this._readers) {
            this.readFrom(reader);
        }
    }

    /**
     * Reads a single line from a single
     * buffered reader. Then attempts to
     * process it as a command.
     * Not blocking
     *
     * @param reader The BufferedReader to read the command line from
     * @throws java.lang.Exception                              Exception thrown by the command
     * @throws com.mt.easytv.commands.CommandNotFoundException  Failed to find the command to execute
     * @throws com.mt.easytv.commands.ArgumentNotFoundException Argument failed parsing or is not allowed value
     */
    public void readFrom(@NotNull BufferedReader reader) throws Exception {
        if (reader.ready()) {
            String line = reader.readLine();

            if (line != null) {
                this.processCommand(line);
            }
        }
    }

    /**
     * Closes all readers then removes them
     * all from the list of command sources
     *
     * @throws java.io.IOException Failed closing a reader
     */
    public void clearReaders() throws IOException {
        for (BufferedReader reader : this._readers) {
            reader.close();
        }

        this._readers.clear();
    }

    private void _checkCommandArguments(Command command, CommandArgumentList args) throws ArgumentNotFoundException {
        if (!command.allowNullArgument) {
            for (CommandArgument arg : args) {
                if (arg.argument == null) {
                    throw new ArgumentNotFoundException(arg.value.toString(), command);
                }
            }
        }
    }

    private Command _findCommand(String command, CommandArgumentList args, boolean clientCommand) throws CommandNotFoundException, ArgumentNotFoundException {
        for (Command possibleCommand : this._commands) {
            if (possibleCommand.command.equals(command) &&
                (
                        (possibleCommand.source == CommandSource.CLIENT && clientCommand) ||
                        (possibleCommand.source == CommandSource.CLI && !clientCommand) ||
                        possibleCommand.source == CommandSource.ANY
                )
                    ) {
                if (!possibleCommand.allowNullArgument) {
                    this._checkCommandArguments(possibleCommand, args);
                }

                return possibleCommand;
            }
        }

        throw new CommandNotFoundException(command);
    }
}
package com.mt.easytv.commands;

import com.mt.easytv.connection.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public final class CommandHandler extends Thread
{
    private ArrayList<Command>        _commands = new ArrayList<>();
    private ArrayList<BufferedReader> _readers  = new ArrayList<>();

    public void addCommand(Command command) {
        this._commands.add(command);
    }

    public void addCommand(CommandSource source, String commandName, ICommand action) {
        this.addCommand(source, commandName, action, true);
    }

    public void addCommand(CommandSource source, String commandName, ICommand action, boolean allowNullArgument) {
        Command command = new Command()
        {
            @Override
            public void processCommand(CommandArgument[] args) throws Exception {
                action.processCommand(args);
            }
        };
        command.source = source;
        command.command = commandName;
        command.allowNullArgument = allowNullArgument;

        this.addCommand(command);
    }

    public void addCommand(CommandSource source, String commandName, IClientCommand action) {
        this.addCommand(source, commandName, action, true);
    }

    public void addCommand(CommandSource source, String commandName, IClientCommand action, boolean allowNullArgument) {
        Command command = new Command()
        {
            @Override
            public Object processCommand(CommandArgument[] args, Client client) throws Exception {
                return action.processCommand(args, client);
            }
        };
        command.source = source;
        command.command = commandName;
        command.allowNullArgument = allowNullArgument;

        this.addCommand(command);
    }

    public boolean removeCommand(String commandName) {
        for (Command command : this._commands) {
            if (commandName.equals(command.command)) {
                this.removeCommand(command);
                return true;
            }
        }

        return false;
    }

    public boolean removeCommand(Command command) {
        if (!this._commands.contains(command)) {
            return false;
        }

        this._commands.remove(command);
        return true;
    }

    public void processCommand(String commandFull) throws Exception {
        /* Building up argument array */
        String[] commandParts = commandFull.split(" ");

        if (commandParts.length < 1) {
            throw new CommandNotFoundException("");
        }

        String[] argumentParts = new String[commandParts.length - 1];
        System.arraycopy(commandParts, 1, argumentParts, 0, commandParts.length - 1);
        CommandArgument[] args = CommandArgument.fromArray(argumentParts);

        boolean commandRan = false;

        for (Command command : this._commands) {
            if (command.source == CommandSource.CLI && command.command.equals(commandParts[0])) {
                this._processCommandArguments(command, args);
                command.processCommand(args);
                commandRan = true;
                break;
            }
        }

        if (!commandRan) {
            throw new CommandNotFoundException(commandParts[0]);
        }
    }

    public Object processCommand(String commandFull, Client client) throws Exception {
        /* Building up argument array */
        String[] commandParts = commandFull.split(" ");

        if (commandParts.length < 1) {
            throw new CommandNotFoundException("");
        }

        String[] argumentParts = new String[commandParts.length - 1];
        System.arraycopy(commandParts, 1, argumentParts, 0, commandParts.length - 1);
        CommandArgument[] args = CommandArgument.fromArray(argumentParts);

        for (Command command : this._commands) {
            if (command.source == CommandSource.CLIENT && command.command.equals(commandParts[0])) {
                this._processCommandArguments(command, args);
                return command.processCommand(args, client);
            }
        }

        throw new CommandNotFoundException(commandParts[0]);
    }

    public void addReader(BufferedReader reader) {
        this._readers.add(reader);
    }

    public boolean removeReader(BufferedReader reader) {
        if (!this._readers.contains(reader)) {
            return false;
        }

        this._readers.remove(reader);
        return true;
    }

    public void read() throws Exception {
        for (BufferedReader reader : this._readers) {
            this.readFrom(reader);
        }
    }

    public void readFrom(BufferedReader reader) throws Exception {
        String line = reader.readLine();

        if (line != null) {
            this.processCommand(line);
        }
    }

    public void clearListeners() throws IOException {
        for (BufferedReader reader : this._readers) {
            reader.close();
        }

        this._readers.clear();
    }

    private void _processCommandArguments(Command command, CommandArgument[] args) throws ArgumentNotFoundException {
        if (!command.allowNullArgument) {
            for (CommandArgument arg : args) {
                if (arg.argument == null) {
                    throw new ArgumentNotFoundException(arg.value, command);
                }
            }
        }
    }

    public enum CommandSource
    {
        CLIENT,
        CLI
    }
}
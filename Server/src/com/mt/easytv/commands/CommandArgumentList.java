package com.mt.easytv.commands;

import com.mt.easytv.interaction.Messager;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import uk.co.maxtingle.communication.common.Message;

import java.util.ArrayList;

public class CommandArgumentList extends ArrayList<CommandArgument>
{
    /**
     * Creates a command argument list from message params, all params in message must be castable to CommandArgument
     *
     * @param message The message to load the CommandArgumentList for
     * @return The list of command arguments
     */
    public static CommandArgumentList fromMessage(@NotNull Message message) {
        CommandArgumentList list = new CommandArgumentList();

        try {
            if (message.params == null) {
                return list;
            }

            for (Object param : message.params) {
                CommandArgument argument = (CommandArgument) param;
                list.add(argument);
            }
        }
        catch (Exception e) {
            Messager.immediateMessage("Invalid message params received in message " + message.toString());
            return null;
        }

        return list;
    }

    /**
     * Checks if the CommandArgumentList has a specified argument in it
     *
     * @param argument The argument to look for
     * @return Whether or not the list contains the given command
     */
    public boolean hasArgument(@NotNull String argument) {
        for (CommandArgument arg : this) {
            if (argument.equals(arg.argument)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the CommandArgumentList has all of the arguments specified
     *
     * @param arguments The arguments to look for
     * @return Whether or not the list contains all the given arguments
     */
    public boolean hasArgument(@NotNull String[] arguments) {
        for (String argument : arguments) {
            if (!this.hasArgument(argument)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the CommandArgumentList has a specified argument in it
     *
     * @param argument The argument to look for
     * @return Whether or not the list contains the given argument
     */
    public boolean hasArgument(@NotNull CommandArgument argument) {
        return this.hasArgument(argument.argument);
    }

    /**
     * Checks if the CommandArgumentList has all of the command arguments another CommandArgumentList has
     *
     * @param arguments The arguments to look for
     * @return Whether or not the list contains all the given arguments
     */
    public boolean hasArgument(@NotNull CommandArgumentList arguments) {
        for (CommandArgument command : arguments) {
            if (!this.hasArgument(command)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the value of a single command argument
     *
     * @param argument The name of the argument to get the value for
     * @return The value or null if it's not found
     */
    public String getValue(@NotNull String argument) {
        return this.getValue(argument, false);
    }

    /**
     * Gets the value of a single command argument or a default value
     *
     * @param argument The name of the argument to get the value for
     * @param def      The default value to return if the argument is not found
     * @return The value of the argument if it's found or the default value
     */
    public String getValue(@NotNull String argument, @Nullable Object def) {
        return (String) this.getValue(argument, def, false);
    }

    /**
     * Gets the value of a single command argument or a default value, with optional handling of null values
     *
     * @param argument The name of the argument to get the value for
     * @param orNull   Whether or not to ignore a null command argument value
     * @return The value of the argument if it's found or the default value
     */
    public String getValue(@NotNull String argument, @NotNull boolean orNull) {
        return (String) this.getValue(argument, null, orNull);
    }

    /**
     * Gets the value of a single command argument or a default value, with optional handling of null values and a default value
     *
     * @param argument The name of the argument to get the value for
     * @param def      The default value to return if the argument is not found
     * @param orNull   Whether or not to ignore a null command argument value
     * @return The value of the argument if it's found or the default value
     */
    public Object getValue(@NotNull String argument, @Nullable Object def, @Nullable boolean orNull) {
        CommandArgument arg = this.get(argument, orNull);

        return arg == null ? def : arg.value;
    }

    /**
     * Gets a single CommandArgument from the list of command arguments
     *
     * @param argument The name of the argument to get the CommandArgument for
     * @return The associated command argument or null if it's not found
     */
    public CommandArgument get(@NotNull String argument) {
        return this.get(argument, false);
    }

    /**
     * Gets a single CommandArgument from the list of command arguments with handling for null values
     *
     * @param argument The name of the argument to get the CommandArgument for
     * @param orNull   Whether or not to ignore a null command argument value
     * @return The associated command argument or null if it's not found
     */
    public CommandArgument get(@NotNull String argument, @Nullable boolean orNull) {
        for (int i = 0; i < super.size(); i++) {
            CommandArgument arg = super.get(i);
            if ((orNull && arg == null) || argument.equals(arg.argument)) {
                return arg;
            }
        }

        return null;
    }
}

package com.mt.easytv.commands;

import java.util.ArrayList;

public class CommandArgumentList extends ArrayList<CommandArgument>
{
    public boolean hasCommand(String argument) {
        for (CommandArgument arg : this) {
            if (argument.equals(arg.argument)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasCommands(String[] arguments) {
        for (String argument : arguments) {
            if (!this.hasCommand(argument)) {
                return false;
            }
        }

        return true;
    }

    public boolean hasCommand(CommandArgument command) {
        return this.hasCommand(command.argument);
    }

    public boolean hasCommand(CommandArgumentList commands) {
        for (CommandArgument command : commands) {
            if (!this.hasCommand(command)) {
                return false;
            }
        }

        return true;
    }

    public String getValue(String argument) {
        return this.getValue(argument, false);
    }

    public String getValue(String argument, Object def) {
        return (String) this.getValue(argument, def, false);
    }

    public String getValue(String argument, boolean orNull) {
        return (String) this.getValue(argument, null, orNull);
    }

    public Object getValue(String argument, Object def, boolean orNull) {
        CommandArgument arg = this.get(argument, orNull);

        return arg == null ? def : arg.value;
    }

    public CommandArgument get(String argument) {
        return this.get(argument, false);
    }

    public CommandArgument get(String argument, boolean orNull) {
        for (int i = 0; i < super.size(); i++) {
            CommandArgument arg = super.get(i);
            if ((orNull && arg == null) || argument.equals(arg.argument)) {
                return arg;
            }
        }

        return null;
    }
}

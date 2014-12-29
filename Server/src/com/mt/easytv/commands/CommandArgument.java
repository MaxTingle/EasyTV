package com.mt.easytv.commands;

import java.util.ArrayList;

public final class CommandArgument implements Cloneable
{
    public String argument;
    public String value;

    /* No nullable params or default values, stupid java >_> */
    public CommandArgument() {
    }

    public CommandArgument(String argument, String value) {
        this.argument = argument;
        this.value = value;
    }

    public static CommandArgument[] fromArray(String[] args) throws ArgumentNotFoundException {
        ArrayList<CommandArgument> commandArguments = new ArrayList<>();
        CommandArgument currentCommand = new CommandArgument();
        boolean previousWasString = false;

        /* For each of the command split via space */
        for (String arg : args) {
            if (previousWasString || (currentCommand.value == null && !arg.startsWith("-"))) { //is value, put first so will support - inside strings
                currentCommand.value = (currentCommand.value == null ? "" : currentCommand.value) + (previousWasString ? " " : "") + arg.replace("\"", "");

                if (arg.startsWith("\"") && !arg.endsWith("\"")) {
                    previousWasString = true;
                }
                else if (arg.endsWith("\"")) {
                    previousWasString = false;
                }
            }
            else if (arg.startsWith("-")) { //is argument
                if (currentCommand.argument != null) { //not new command
                    commandArguments.add((CommandArgument) currentCommand.clone());
                    currentCommand = new CommandArgument();
                    previousWasString = false;
                }

                currentCommand.argument = arg.replaceFirst("-", "");
            }
            else { //currentCommand was value for the currentCommand (x2) command
                throw new ArgumentNotFoundException(arg);
            }
        }

        /* Add last command */
        if (currentCommand.argument != null || currentCommand.value != null) {
            commandArguments.add(currentCommand);
        }

        return commandArguments.toArray(new CommandArgument[commandArguments.size()]);
    }

    @Override
    public Object clone() {
        return new CommandArgument(this.argument, this.value);
    }
}
package com.mt.easytv.commands;

import java.util.ArrayList;

public class CommandArgument implements Cloneable
{
    public String argument;
    public String value;

    /* No nullable params or default values, stupid java >_> */
    public CommandArgument()
    {
    }

    public CommandArgument(String argument, String value)
    {
        this.argument = argument;
        this.value = value;
    }

    public static CommandArgument[] fromArray(String[] args) throws ArgumentNotFoundException
    {
        ArrayList<CommandArgument> commandArguments = new ArrayList<>();
        CommandArgument currentCommand = new CommandArgument();

        /* For each of the command split via space */
        for (String arg : args) {
            /* Is argument */
            if (arg.startsWith("-")) {
                if (currentCommand.argument != null) { //not new command
                    commandArguments.add((CommandArgument) currentCommand.clone());
                    currentCommand = new CommandArgument();
                }

                currentCommand.argument = arg.replaceFirst("-", "");
            } else if (currentCommand.value == null) { //is value
                currentCommand.value += arg; //in-case the value has spaces in it, will be reset on net command anyway
            } else { //currentCommand was value for the currentCommand (x2) command
                throw new ArgumentNotFoundException(arg);
            }
        }

        /* Add last command */
        if (currentCommand.argument != null) {
            commandArguments.add(currentCommand);
        }

        return commandArguments.toArray(new CommandArgument[commandArguments.size()]);
    }

    @Override
    public Object clone()
    {
        return new CommandArgument(this.argument, this.value);
    }
}

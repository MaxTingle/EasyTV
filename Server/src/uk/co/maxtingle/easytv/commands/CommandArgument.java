package uk.co.maxtingle.easytv.commands;

/**
 * The container class for a single command argument
 * and the value associated with it. Also contains
 * a method for generating an array of its self based
 * upon an array of the command split by space.
 */
public final class CommandArgument implements Cloneable
{
    /**
     * The name of the argument, can not include dashes or spaces.
     */
    public String argument;

    /**
     * The value of the argument, can include any character
     * including dashes and spaces. Surround with double quotes
     * to use dashes or spaces.
     */
    public Object value;

    /**
     * Dummy constructor to allow no argument construction
     */
    public CommandArgument() {
    }

    /**
     * Sets the class variables argument and value respectively
     */
    public CommandArgument(String argument, Object value) {
        this.argument = argument;
        this.value = value;
    }

    /**
     * Generates an array of command arguments from an array of
     * the parts from the command being split via spaces
     *
     * @param args The arguments split by space
     * @return The arguments, re-joined, filled out into an array of
     * the name of the argument and the value associated.
     * Can have null argument but value to allow for
     * {command} {implicit arg value}
     * Can also have null value to allow for boolean args like
     * {command} -{arg name}
     */
    public static CommandArgumentList fromArray(String[] args) throws ArgumentNotFoundException {
        CommandArgumentList commandArguments = new CommandArgumentList();
        CommandArgument currentCommand = new CommandArgument();
        boolean previousWasString = false;

        /* For each of the command split via space */
        for (String arg : args) {
            if (!previousWasString && arg.startsWith("-")) { //is argument
                if (currentCommand.argument != null || currentCommand.value != null) { //is a new command
                    commandArguments.add((CommandArgument) currentCommand.clone());
                    currentCommand = new CommandArgument();
                    previousWasString = false;
                }

                currentCommand.argument = arg.replaceFirst("-", "");
            }
            else {
                currentCommand.value = (currentCommand.value == null ? "" : currentCommand.value) + (previousWasString ? " " : "") + arg.replace("\"", "");

                if (arg.startsWith("\"") && !arg.endsWith("\"")) {
                    previousWasString = true;
                }
                else if (arg.endsWith("\"")) {
                    previousWasString = false;
                }
            }
        }

        /* Add last command */
        if (currentCommand.argument != null || currentCommand.value != null) {
            commandArguments.add(currentCommand);
        }

        return commandArguments;
    }

    @Override
    /**
     * Creates a new instance of the CommandArgument class
     *
     * @return A new instance of the CommandArgument class
     *         with argument and value the same as the current
     *         values in this class.
     */
    public Object clone() {
        return new CommandArgument(this.argument, this.value);
    }
}
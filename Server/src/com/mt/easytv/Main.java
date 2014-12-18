package com.mt.easytv;

import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.commands.CommandHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        CommandHandler commandHandler = new CommandHandler();
        Main._addCommands(commandHandler);

        try {
            Main._processExecCommands(commandHandler, args);
            commandHandler.addReader(new BufferedReader(new InputStreamReader(System.in)));
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }

        while(true) {
            commandHandler.read();
        }
    }

    private static void _addCommands(CommandHandler handler) {
        handler.addCommand("quit", (CommandArgument[] args) -> System.exit(0));
    }

    private static void _processExecCommands(CommandHandler handler, String[] args) throws Exception {
        for(String arg : args) {
            if(arg.startsWith("-")) {
                handler.processCommand(arg.replaceFirst("-", ""));
            }
        }
    }
}

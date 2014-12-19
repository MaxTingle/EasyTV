package com.mt.easytv.commands;

public class CommandNotFoundException extends Exception
{
    private String _command;

    public CommandNotFoundException(String command)
    {
        this._command = command;
    }

    @Override
    public String getMessage()
    {
        return "Command " + this._command + " not found.";
    }
}
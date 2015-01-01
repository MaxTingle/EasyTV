package com.mt.easytv.commands;

public final class ArgumentNotFoundException extends Exception
{
    private String  _value;
    private Command _command;

    public ArgumentNotFoundException(String value, Command command) {
        this._value = value;
        this._command = command;
    }

    @Override
    public String getMessage() {
        return "Argument '" + this._value + "' is not a valid argument for the command '" + this._command.command + "'";
    }
}
package com.mt.easytv.commands;

public class ArgumentNotFoundException extends Exception
{
    private String _value;

    public ArgumentNotFoundException(String value)
    {
        this._value = value;
    }

    @Override
    public String getMessage()
    {
        return "Argument " + this._value + " is not a valid command.";
    }
}
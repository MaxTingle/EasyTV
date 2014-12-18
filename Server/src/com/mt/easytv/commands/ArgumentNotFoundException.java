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
        return "argument " + this._value + super.getMessage();
    }
}
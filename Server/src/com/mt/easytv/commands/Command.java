package com.mt.easytv.commands;

public abstract class Command implements ICommand
{
    public String command = "";

    public abstract void processCommand(CommandArgument[] args);
}
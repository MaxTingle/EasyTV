package com.mt.easytv.commands;

public abstract class Command
{
    public String Command = "";
    public abstract void processCommand(CommandArgument[] args);
}
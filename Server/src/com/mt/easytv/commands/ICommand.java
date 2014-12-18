package com.mt.easytv.commands;

@FunctionalInterface
public interface ICommand
{
    void processCommand(CommandArgument[] args);
}

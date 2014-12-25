package com.mt.easytv.commands;

import com.mt.easytv.connection.Client;

@FunctionalInterface
public interface IClientCommand
{
    Object processCommand(CommandArgument[] args, Client client) throws Exception;
}
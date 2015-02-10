package com.mt.easytv.connectivity;

import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.commands.CommandArgumentList;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

public class ClientMessage
{
    public  String              request;
    public  Object[]            args;
    private CommandArgumentList commandArguments;

    public ClientMessage() {
    }

    public ClientMessage(@NotNull String request, @Nullable Object[] args) {
        this.request = request;
        this.args = args;
    }

    public void loadCommandArguments() throws InvalidMessageException {
        this.commandArguments = new CommandArgumentList();

        if (this.args == null) {
            return;
        }

        for (int i = 0; i < this.args.length; i++) {
            CommandArgument commandArgument = new CommandArgument();

            if (this.args[i] == null) {
                throw new InvalidMessageException(null, commandArgument.getClass().getName());
            }

            this.commandArguments.add((CommandArgument) this.args[i]);
        }
    }

    public CommandArgumentList getCommandArguments() {
        return this.commandArguments;
    }

    public ServerMessage buildReply(boolean success, String response) {
        return this.buildReply(success, response, new CommandArgument[0]);
    }

    public ServerMessage buildReply(boolean success, String response, CommandArgument responseData) {
        return this.buildReply(success, response, new CommandArgument[]{responseData});
    }

    public ServerMessage buildReply(boolean success, String response, CommandArgument[] responseData) {
        ServerMessage message = new ServerMessage(this);
        message.response = response;
        message.success = success;
        message.responseData = responseData;
        return message;
    }
}
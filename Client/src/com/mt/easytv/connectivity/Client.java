package com.mt.easytv.connectivity;

public class Client
{
    public ServerMessage run(String command, CommandArgument[] args) {
        return this.run(new ClientMessage(command, args));
    }

    public ServerMessage run(ClientMessage message) {
        return null;
    }
}

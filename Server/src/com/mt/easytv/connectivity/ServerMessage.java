package com.mt.easytv.connectivity;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

public class ServerMessage
{
    public String   command;
    public Object[] args;
    public boolean  success;
    public String   response;
    public Object[] responseData;

    public ServerMessage() {

    }

    public ServerMessage(@NotNull String command, @Nullable Object[] args) {
        this.command = command;
        this.args = args;
    }

    public ServerMessage(@NotNull String command, @Nullable Object[] args, @NotNull boolean success, @Nullable String response, @Nullable Object[] responseData) {
        this(command, args);
        this.success = success;
        this.response = response;
        this.responseData = responseData;
    }
}
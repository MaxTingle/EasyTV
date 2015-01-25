package com.mt.easytv.connectivity;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

public class ServerMessage
{
    public String   request;
    public Object[] args;
    public boolean  success;
    public String   response;
    public Object[] responseData;

    public ServerMessage() {

    }

    public ServerMessage(@NotNull String request, @Nullable Object[] args) {
        this.request = request;
        this.args = args;
    }

    public ServerMessage(@NotNull String request, @Nullable Object[] args, @NotNull boolean success, @Nullable String response, @Nullable Object[] responseData) {
        this(request, args);
        this.success = success;
        this.response = response;
        this.responseData = responseData;
    }
}
package com.mt.easytv.connectivity;

public class ServerMessage
{
    public String   request;
    public Object[] args;
    public boolean  success;
    public String   response;
    public Object[] responseData;

    public ServerMessage(String request, Object[] args, boolean success, String response, Object[] responseData) {
        this.request = request;
        this.args = args;
        this.success = success;
        this.response = response;
        this.responseData = responseData;
    }
}
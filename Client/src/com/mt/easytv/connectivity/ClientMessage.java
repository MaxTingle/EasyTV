package com.mt.easytv.connectivity;

public class ClientMessage
{
    public String   request;
    public Object[] args;

    public ClientMessage(String request, Object[] args) {
        this.request = request;
        this.args = args;
    }
}
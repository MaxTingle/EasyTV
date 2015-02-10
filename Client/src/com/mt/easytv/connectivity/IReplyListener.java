package com.mt.easytv.connectivity;

public interface IReplyListener
{
    public void onReply(ServerMessage reply, ClientMessage original);
}

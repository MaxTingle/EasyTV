package com.mt.easytv.connectivity;

import java.util.ArrayList;

public class ClientMessage
{
    public  String                    request;
    public  Object[]                  args;
    private ArrayList<IReplyListener> _replyListeners;
    private boolean _sent = false;

    public ClientMessage(String request, Object[] args) {
        this.request = request;
        this.args = args;
        this._replyListeners = new ArrayList<>();
    }

    public void onReply(IReplyListener listener) throws Exception {
        if (this._replyListeners.contains(listener)) {
            throw new Exception("Reply listener already added");
        }

        this._replyListeners.add(listener);
    }

    public void handleReply(ServerMessage reply) {
        for (IReplyListener listener : this._replyListeners) {
            listener.onReply(reply, this);
        }
    }

    public boolean isReplyTo(ServerMessage reply) {
        return reply.request.equals(this.request) && reply.args == this.args;
    }

    public boolean hasSent() {
        return this._sent;
    }

    public void send() throws Exception {
        if (this._sent) {
            return;
        }

        Client.addToQue(this);
    }
}
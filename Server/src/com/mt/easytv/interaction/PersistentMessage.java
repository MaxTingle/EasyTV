package com.mt.easytv.interaction;

public abstract class PersistentMessage implements IPersistentMessage
{
    Object _associated;
    String _previousMessage;

    public void updateMessage() {
        this._previousMessage = this.updateMessage(this._previousMessage);
    }

    public abstract String updateMessage(String previousMessage);

    public boolean equals(Object associated) {
        return this == associated || (this._associated != null && this._associated == associated);
    }
}
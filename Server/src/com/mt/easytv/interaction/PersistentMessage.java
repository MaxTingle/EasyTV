package com.mt.easytv.interaction;

/**
 * A message which has content that updates, such as a progress bar
 */
public abstract class PersistentMessage implements IPersistentMessage
{
    Object _associated;
    String _previousMessage;

    /**
     * Updates the message string using the implemented updateMessage method
     */
    public void updateMessage() {
        this._previousMessage = this.updateMessage(this._previousMessage);
    }

    /**
     * Updates the message string to reflect a change, EG: A progress bar going from 99% to 100%
     */
    public abstract String updateMessage(String previousMessage);

    /**
     * Checks if the object is a reference to this object or if they are linked to the same object
     */
    public boolean equals(Object associated) {
        return this == associated || (this._associated != null && this._associated == associated);
    }
}
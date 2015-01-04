package com.mt.easytv.interaction;

public final class Progress
{
    private double _total;
    private double _processed = 0;
    private String _prefix    = "";
    private PersistentMessage _persistentMessage;

    public Progress(double total) {
        this._total = total;
    }

    public Progress(double total, String prefix) {
        this._total = total;
        this._prefix = prefix;
    }

    public void update(double processed) {
        this._processed = processed;
    }

    public void increment() {
        this._processed++;
    }

    public String getProcessed() {
        return ((int) this._processed) + "/" + ((int) this._total);
    }

    public double getPercent() {
        return (this._processed / this._total) * 100;
    }

    public boolean show() {
        if (this._persistentMessage != null || Messager.getPersistentMessage(this) != null) {
            return false;
        }

        Progress self = this;
        this._persistentMessage = Messager.addPersistentMessage((String previous) -> self._prefix + " " + self.getProcessed() +
                                                                                     " (" + String.format("%.2f", self.getPercent()) + "%)",
                                                                this);

        return true;
    }

    public boolean hide() {
        if (this._persistentMessage == null) {
            return false;
        }

        this._persistentMessage.updateMessage();
        return Messager.removePersistentMessage(this._persistentMessage);
    }
}
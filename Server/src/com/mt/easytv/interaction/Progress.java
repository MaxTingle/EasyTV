package com.mt.easytv.interaction;

public class Progress
{
    private double _total;
    private double _processed = 0;
    private String _prefix    = "";

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

    public void display() {
        Messager.message(this._prefix + " " + this.getProcessed() + " (" + String.format("%.2f", this.getPercent()) + "%)");
    }
}
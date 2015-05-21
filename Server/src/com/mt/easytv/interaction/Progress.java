package com.mt.easytv.interaction;

/**
 * A wrapper around the Messager's PersistentMessage to allow
 * easy progress showing and updating.
 */
public final class Progress
{
    private double _total;
    private double _processed = 0;
    private String _prefix    = "";
    private PersistentMessage _persistentMessage;

    /**
     * Creates a Progress instance and sets the total
     *
     * @param total The maximum number of things to be done by whatever is progressing
     */
    public Progress(double total) {
        this._total = total;
    }

    /**
     * Creates a Progress instance and sets the total and prefix
     *
     * @param total  The maximum number of things to be done by whatever is progressing
     * @param prefix A message to show infront of the PersistentMessage's normal percent processed message
     */
    public Progress(double total, String prefix) {
        this._total = total;
        this._prefix = prefix;
    }

    /**
     * Updates the amount progressed so the Messager's next
     * redraw call will reflect the real amount progressed
     *
     * @param processed The amount out of the total processed
     */
    public void update(double processed) {
        this._processed = processed;
    }

    /**
     * Increments the total number of items processed
     */
    public void increment() {
        this._processed++;
    }

    /**
     * Gets a string representation of the number of processed items
     * verses the total number of items
     *
     * @return The string representation of the progress
     */
    public String getProcessed() {
        return ((int) this._processed) + "/" + ((int) this._total);
    }

    /**
     * Gets the percent of the total items that have been processed
     *
     * @return The percent of the items processed
     */
    public double getPercent() {
        return (this._processed / this._total) * 100;
    }

    /**
     * Attempts to attach the Progress to the Messager system
     *
     * @return Whether or not the Progress was successfully attached
     */
    public boolean show() {
        if (this._persistentMessage != null || Messager.getPersistentMessage(this) != null) {
            return false;
        }

        this._persistentMessage = Messager.addPersistentMessage((String previous) -> Progress.this._prefix + " " + Progress.this.getProcessed() +
                                                                                     " (" + String.format("%.2f", Progress.this.getPercent()) + "%)",
                                                                this);

        return true;
    }

    /**
     * Detaches the associated PersistentMessage from the Messager's
     * PersistentMessaging system
     *
     * @return Whether or not the Progress was successfully detached, it may not succeed if it wasn't already attached
     */
    public boolean hide() {
        if (this._persistentMessage == null) {
            return false;
        }

        this._persistentMessage.updateMessage();
        return Messager.removePersistentMessage(this._persistentMessage);
    }
}
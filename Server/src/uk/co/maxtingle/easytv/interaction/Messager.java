package uk.co.maxtingle.easytv.interaction;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import uk.co.maxtingle.communication.common.BaseClient;
import uk.co.maxtingle.communication.common.Message;
import uk.co.maxtingle.communication.debug.Debugger;
import uk.co.maxtingle.communication.server.ServerClient;
import uk.co.maxtingle.easytv.Main;
import uk.co.maxtingle.easytv.torrents.Torrent;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * The messaging system for the system to speak to clients and log events happening
 * also services as an error logging system
 */
public final class Messager
{
    public static final String LINE_SEPERATOR = System.getProperty("line.separator");
    private static String _outputBuffer = "";
    private static ArrayList<PersistentMessage> _persistentMessages = new ArrayList<>();
    private static Thread _redrawThread;
    private static boolean _autoRedrawing = false;

    /**
     * Immediately prints a line to the console and adds it to the buffer
     * For use in startup when the Messager hasn't started redrawing or for
     * critical errors
     *
     * @param msg The message to display
     */
    public static void immediateMessage(@NotNull String msg) {
        System.out.println(msg);
        Messager.addBufferLine(msg);
    }

    /**
     * Sends a clone of the message to every accepted client,
     * each message sent will have a different id and thus bring back
     * a different ServerClient on its onReply
     *
     * @param message The message to send
     */
    public static void messageAllClients(@NotNull Message message) throws Exception {
        for (ServerClient client : Main.server.getAcceptedClients()) {
            client.sendMessage(message.clone());
        }
        Debugger.log("App", "Sent message to all clients");
    }

    /**
     * Tells all the clients that a torrent has been changed
     * the clients should then reload the entire torrent but
     * could choose to only reload the state and progress so
     * if you change the name or url consider removing the torrent
     * <p>
     * This method sends the entire torrent so clients can know
     * about a torrent if they didn't already have track of it
     *
     * @param torrent The torrent that has been updated
     */
    public static void informClientsAboutChange(@NotNull Torrent torrent) {
        try {
            Messager.messageAllClients(new Message("__TORRENT_UPDATE__", new Object[]{torrent}));
            Debugger.log("App", "Sent torrent update to all clients for torrent " + torrent.id);
        }
        catch (Exception e) {
            Messager.message("Failed to update clients about torrent change: " + e.getMessage());
        }
    }

    /**
     * Informs a client about a torrent that is in a notable state
     * Used when a client connects to make sure they know about all
     * our torrents
     *
     * @param client  The client to inform
     * @param torrent The torrent that has been updated
     */
    public static void informClientAboutTorrent(BaseClient client, @NotNull Torrent torrent) {
        try {
            client.sendMessage(new Message("__TORRENT_UPDATE__", new Object[]{torrent}));
        }
        catch (Exception e) {
            Messager.message("Failed to update clients about torrent change: " + e.getMessage());
        }
    }

    /**
     * Tells all the clients that a torrent error has occurred
     * They will most likely show this message to the user
     *
     * @param error The error that has occurred
     */
    public static void informClientsAboutError(@Nullable String error) {
        try {
            Messager.messageAllClients(new Message("__TORRENT_ERROR__", new String[]{error}));
            Debugger.log("App", "Sent error to all clients: " + error);
        }
        catch (Exception e) {
            Messager.message("Failed to inform clients about the error '" + error + "', " + e.getMessage());
        }
    }

    /**
     * Tells all the clients that a torrent has been removed from the
     * list of loaded torrents
     *
     * @param torrent The torrent that has been removed
     */
    public static void informClientsAboutRemoval(@NotNull Torrent torrent) {
        Messager.informClientsAboutRemoval(torrent.id);
    }

    /**
     * Tells all the clients that a torrent has been removed from the
     * list of loaded torrents
     *
     * @param id The id of the torrent that has been removed
     */
    public static void informClientsAboutRemoval(@NotNull String id) {
        try {
            Messager.messageAllClients(new Message("__TORRENT_REMOVE__", new String[]{id}));
            Messager.message("Sent torrent removal to all clients for torrent " + id);
        }
        catch (Exception e) {
            Messager.message("Failed to inform clients about torrent removal '" + e.getMessage());
        }
    }

    /**
     * Adds a message to the buffer to be shown on the next redraw
     *
     * @param msg The message to show
     */
    public static void message(@NotNull String msg) {
        Messager.addBufferLine(msg);
    }

    /**
     * Converts the exception, including stack trace, to a string and then
     * uses the message function to add the message to the buffer to be
     * shown on the next redraw
     *
     * @param prefix A prefix to go before the error message
     * @param e      The exception to print to the console
     */
    public static void error(@Nullable String prefix, @NotNull Exception e) {
        String message = prefix + " " + e.getClass().getSimpleName() + " exception " + e.getMessage();

        for (StackTraceElement stack : e.getStackTrace()) {
            message += Messager.LINE_SEPERATOR + "in " + stack.getFileName() + " " + stack.getClassName() + "." + stack.getMethodName() + " on line " + stack.getLineNumber();
        }

        Messager.message(message);
    }

    /**
     * Adds a line directly to the output buffer, plus a newline tag
     *
     * @param line The line to write to the output buffer
     */
    public static void addBufferLine(@NotNull String line) {
        Messager._outputBuffer += line + Messager.LINE_SEPERATOR;
    }

    /**
     * Gets a persistent message from its associated object
     *
     * @param associated The object associated to the PersistentMessage
     * @return The first PersistentMessage associated with the object or null if it's not found
     */
    public static PersistentMessage getPersistentMessage(@NotNull Object associated) {
        for (PersistentMessage message : Messager._persistentMessages) {
            if (message.equals(associated)) {
                return message;
            }
        }

        return null;
    }

    /**
     * Adds a PersistentMessage to the list of messages to be redrawn every
     * buffer redraw
     *
     * @param action The PersistentMessage to add
     */
    public static PersistentMessage addPersistentMessage(@NotNull IPersistentMessage action) {
        return Messager.addPersistentMessage(action, null);
    }

    /**
     * Adds a PersistentMessage to the list of messages to be redrawn every
     * buffer redraw and associates an object with it to find allow you to
     * get the PersistentMessage using getPersistentMessage later
     *
     * @param action     The PersistentMessageto add
     * @param associated The object associated with the PersistentMessage, must not be used on any other PersistentMessages
     */
    public static PersistentMessage addPersistentMessage(@NotNull IPersistentMessage action, @Nullable Object associated) {
        PersistentMessage persistentMessage = new PersistentMessage()
        {
            @Override
            public String updateMessage(String previousMessage) {
                return action.updateMessage(previousMessage);
            }
        };
        persistentMessage._associated = associated;

        Messager._persistentMessages.add(persistentMessage);
        return persistentMessage;
    }

    /**
     * Removes a PersistentMessage from the list of redrawn PersistentMessages,
     * its last message will be added to the end of the output buffer and
     * previous messages will be removed
     *
     * @param persistentMessage The PersistentMessage to remove
     * @return Whether or not the PersistentMessage was successfully found and removed
     */
    public static boolean removePersistentMessage(@NotNull PersistentMessage persistentMessage) {
        if (!Messager.persistentMessagesContain(persistentMessage)) {
            return false;
        }

        Messager.addBufferLine(persistentMessage._previousMessage);
        return Messager._persistentMessages.remove(persistentMessage);
    }

    /**
     * Adds the latest updated line from each PersistentMessage to the buffer output
     * then removes them all from the list of PersistentMessages.
     * See removePersistentMessage for more detail
     */
    public static void removeAllPersistentMessages() {
        Messager._persistentMessages.forEach(Messager::removePersistentMessage);
    }

    /**
     * Clears the output buffer
     */
    public static void clearBuffer() {
        Messager._outputBuffer = "";
    }

    /**
     * Clears and then draws the output buffer
     * after updating all the PersistentMessage's text
     */
    public static void redrawBuffer() {
        Messager.clearOutput();
        System.out.print(Messager._outputBuffer);

        /* Update persistent messages */
        String persistentMessageBuffer = "";

        for (PersistentMessage message : Messager._persistentMessages) {
            message.updateMessage();
            persistentMessageBuffer += message._previousMessage + "\n";
        }

        System.out.print(persistentMessageBuffer);
    }

    /**
     * Spawns a new thread (To avoid blocking) which will redraw the buffer
     * then sleep for the interval time. May raise a Messager error if the
     * worker thread is interrupted when stopRedrawing has not been called.
     *
     * @param interval The number of milliseconds to sleep for
     * @throws Exception Already running
     */
    public static void startRedrawing(final int interval) throws Exception {
        if (Messager._autoRedrawing || (Messager._redrawThread != null && Messager._redrawThread.isAlive() && !Messager._redrawThread.isInterrupted())) {
            throw new Exception("Redrawing is already in progress");
        }

        Messager._redrawThread = new Thread(() -> {
            Messager._autoRedrawing = true;

            while (Messager._autoRedrawing) {
                Messager.redrawBuffer();

                try {
                    Thread.sleep(interval);
                }
                catch (InterruptedException e) {
                    if (Messager._autoRedrawing) {
                        Messager.error("Redrawing thread interrupted while supposed to be running", e);
                    }
                }
            }
        });

        Messager._redrawThread.start();
    }

    /**
     * Safely shuts down the Messager's redrawing thread
     */
    public static void stopRedrawing() {
        Messager._autoRedrawing = false;

        if (Messager._redrawThread != null) {
            Messager._redrawThread.interrupt();
        }

        try {
            FileWriter writer = new FileWriter(Main.config.concatValues(new String[]{"storage", "logLocation"}, "\\"), true);

            writer.write("===================================" + Messager.LINE_SEPERATOR);
            writer.write("===================================" + Messager.LINE_SEPERATOR);
            writer.write("SERVER LOG FOR " + SimpleDateFormat.getDateTimeInstance().format(new Date()) + Messager.LINE_SEPERATOR);
            writer.write("===================================" + Messager.LINE_SEPERATOR);
            writer.write("===================================" + Messager.LINE_SEPERATOR);
            writer.write(Messager._outputBuffer);
            writer.close();
        }
        catch (Exception e) {
            Messager.immediateMessage("Failed to save to log file: " + e.getMessage());
        }
    }

    /**
     * Clears the currently outputted messages.
     * Due to restrictions with java, this just prints 11 new lines.
     */
    public static void clearOutput() {
        for (int i = 0; i < 11; i++) {
            System.out.println(""); //Screw this stupid language and its lack of ability to properly clear the console.
        }
    }

    private static boolean persistentMessagesContain(PersistentMessage message) {
        for (PersistentMessage comparison : Messager._persistentMessages) {
            if (message == comparison) {
                return true;
            }
        }

        return false;
    }
}
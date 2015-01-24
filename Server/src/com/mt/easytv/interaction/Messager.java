package com.mt.easytv.interaction;

import com.mt.easytv.connectivity.ServerMessage;
import com.sun.istack.internal.Nullable;

import java.util.ArrayList;

public final class Messager
{
    private static ServerMessage _message; //attaching of client is done so single point of communication for cli and server
    private static String _outputBuffer = "";
    private static String _clearOutputCMD;
    private static ArrayList<PersistentMessage> _persistentMessages = new ArrayList<>();
    private static Thread _redrawThread;
    private static boolean _autoRedrawing = false;

    public static void init() {
        Messager._clearOutputCMD = System.getProperty("os.name").contains("Windows") ? "cls" : "clear";
    }

    public static void attachMessage(ServerMessage message) {
        Messager._message = message;
    }

    public static void detachMessage() {
        Messager._message = null;
    }

    public static void immediateMessage(String msg) {
        System.out.println(msg);
        Messager.message(msg);
    }

    public static void message(String msg) {
        Messager.message(msg, null);
    }

    public static void message(String msg, @Nullable Object[] data) {
        if (Messager._message != null) {
            Messager._message.success = true;
            Messager._message.response = msg;
            Messager._message.responseData = data;
        }
        else {
            Messager.addBufferLine(msg);
        }
    }

    public static void error(String prefix, Exception e) {
        String message = prefix + e.getClass() + " exception " + e.getMessage();

        for (StackTraceElement stack : e.getStackTrace()) {
            message += "\nin " + stack.getFileName() + " " + stack.getClassName() + "." + stack.getMethodName() + " on line " + stack.getLineNumber();
        }

        if (Messager._message != null) {
            Messager._message.success = false;
            Messager._message.response = message;
            Messager._message.responseData = new Object[]{e};
        }
        else {
            Messager.message(message);
        }
    }

    public static void addBufferLine(String line) {
        Messager._outputBuffer += line + "\n";
    }

    public static PersistentMessage getPersistentMessage(Object associated) {
        for (PersistentMessage message : Messager._persistentMessages) {
            if (message.equals(associated)) {
                return message;
            }
        }

        return null;
    }

    public static PersistentMessage addPersistentMessage(IPersistentMessage action) {
        return Messager.addPersistentMessage(action, null);
    }

    public static PersistentMessage addPersistentMessage(IPersistentMessage action, Object associated) {
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

    public static boolean removePersistentMessage(PersistentMessage persistentMessage) {
        if (!Messager.persistentMessagesContain(persistentMessage)) {
            return false;
        }

        Messager.addBufferLine(persistentMessage._previousMessage);
        return Messager._persistentMessages.remove(persistentMessage);
    }

    public static void removeAllPersistentMessages() {
        Messager._persistentMessages.forEach((PersistentMessage message) -> Messager.addBufferLine(message._previousMessage));
        Messager._persistentMessages.clear();
    }

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
                        Messager.error("Redrawing thread interrupted while not supposed to be running", e);
                    }
                }
            }
        });

        Messager._redrawThread.start();
    }

    public static void stopRedrawing() {
        Messager._autoRedrawing = false;

        if (Messager._redrawThread != null) {
            Messager._redrawThread.interrupt();
        }
    }

    public static void clearOutput() {
        try {
            Runtime.getRuntime().exec(Messager._clearOutputCMD);
        }
        catch (Exception e) {
            System.out.flush(); //prints a bunch of /r/n's, messy and ugly but a fallback none the less
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
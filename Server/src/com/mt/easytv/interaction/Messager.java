package com.mt.easytv.interaction;

import java.util.ArrayList;

public final class Messager
{
    private static String _outputBuffer = "";
    private static ArrayList<PersistentMessage> _persistentMessages = new ArrayList<>();
    private static Thread _redrawThread;
    private static boolean _autoRedrawing = false;

    public static void immediateMessage(String msg) {
        System.out.println(msg);
        Messager.addBufferLine(msg);
    }

    public static void message(String msg) {
        Messager.addBufferLine(msg);
    }

    public static void error(String prefix, Exception e) {
        String message = prefix + e.getClass() + " exception " + e.getMessage();

        for (StackTraceElement stack : e.getStackTrace()) {
            message += "\nin " + stack.getFileName() + " " + stack.getClassName() + "." + stack.getMethodName() + " on line " + stack.getLineNumber();
        }

        Messager.message(message);
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

    public static void clearBuffer() {
        Messager._outputBuffer = "";
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
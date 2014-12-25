package com.mt.easytv.interaction;

public class Messager
{
    private static Message _message; //attaching of client is done so single point of communication for cli and server

    public static void attachMessage(Message message) {
        Messager._message = message;
    }

    public static void detachMessage() {
        Messager._message = null;
    }

    public static void message(String msg) //dummy function in-case extra functionality needed later
    {
        if (Messager._message != null) {
            Messager._message.message = msg;
        }
        else {
            System.out.println(msg);
        }
    }

    public static void error(String prefix, Exception e) {
        if (Messager._message != null) {
            String message = prefix + e.getMessage();

            for (StackTraceElement stack : e.getStackTrace()) {
                message += "\nin " + stack.getFileName() + " " + stack.getClassName() + "." + stack.getMethodName() + " on line " + stack.getLineNumber();
            }

            Messager._message.success = false;
            Messager._message.message = message;
        }
        else {
            Messager.message(prefix + e.getMessage());

            for (StackTraceElement stack : e.getStackTrace()) {
                Messager.message("in " + stack.getFileName() + " " + stack.getClassName() + "." + stack.getMethodName() + " on line " + stack.getLineNumber());
            }
        }
    }
}

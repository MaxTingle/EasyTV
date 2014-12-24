package com.mt.easytv.interaction;

public class Messager
{
    public static void message(String error) //dummy function in-case extra functionality needed later
    {
        System.out.println(error);
    }

    public static void error(String prefix, Exception e) {
        Messager.message(prefix + e.getMessage());

        for (StackTraceElement stack : e.getStackTrace()) {
            Messager.message("in " + stack.getFileName() + " " + stack.getClassName() + "." + stack.getMethodName() + " on line " + stack.getLineNumber());
        }
    }
}

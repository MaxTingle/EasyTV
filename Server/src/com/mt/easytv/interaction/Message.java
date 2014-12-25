package com.mt.easytv.interaction;

public class Message implements java.io.Serializable
{
    public boolean success;
    public String  message;
    public Object  data;

    public Message(boolean success) {
        this.success = success;
    }

    public Message(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public Message(boolean success, Object data) {
        this.success = success;
        this.data = data;
    }

    public Message(Boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}

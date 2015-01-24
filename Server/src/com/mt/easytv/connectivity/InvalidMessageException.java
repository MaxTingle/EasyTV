package com.mt.easytv.connectivity;

public class InvalidMessageException extends Exception
{
    private Object _received;
    private String _expected;

    public InvalidMessageException(Object received, String expected) {
        this._received = received;
        this._expected = expected;
    }

    @Override
    public String getMessage() {
        return "Client sent invalid message, excepted instance of " + this._expected + ", got " + (this._received == null ? "null" : this._received.getClass().getName());
    }
}

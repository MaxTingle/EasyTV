package com.mt.easytv.connectivity;

import com.mt.easytv.config.Config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class Server
{
    Socket _socket;
    private InetAddress _address;

    public Server(InetAddress address) throws IOException {
        this._address = address;
        this._socket = new Socket(this._address, Integer.parseInt(Config.getValue("port")));
    }

    public InetAddress getAddress() {
        return _address;
    }

    public void attemptAuth() {
        //TODO: make this send magic, check response magic and then send auth and check auth response
    }
}
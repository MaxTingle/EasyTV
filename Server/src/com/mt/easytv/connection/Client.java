package com.mt.easytv.connection;

import com.mt.easytv.Main;
import com.mt.easytv.interaction.Message;

import java.beans.XMLEncoder;
import java.io.*;
import java.net.Socket;

public class Client extends Thread
{
    private Socket         _socket;
    private BufferedWriter _writer;
    private XMLEncoder     _encoder;
    private BufferedReader _reader;
    private boolean        _accepted;
    private String         _magic;

    public Client(Socket socket) throws IOException {
        this._socket = socket;
        this._writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this._encoder = new XMLEncoder(socket.getOutputStream());
        this._reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public String getLatestMessage() throws IOException {
        return this._reader.readLine();
    }

    public void message(String message) throws IOException {
        this._writer.write(message);
    }

    public void message(Message message) throws IOException {
        this._encoder.writeObject(message);
    }

    public boolean checkMagic() throws IOException {
        if (this._magic == null) {
            this._magic = this._reader.readLine();
        }

        return this._magic.equals(Main.config.getValue("expectedMagic"));
    }

    public void acceptConnection() {
        this._accepted = true;
    }

    public void closeConnection() throws IOException {
        this._writer.close();
        this._reader.close();
        this._socket.shutdownInput();
        this._socket.shutdownOutput();
        this._socket.close();
    }

    public boolean isWaiting() {
        return this._accepted;
    }
}
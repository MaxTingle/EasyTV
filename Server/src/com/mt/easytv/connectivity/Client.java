package com.mt.easytv.connectivity;

import com.mt.easytv.Main;
import com.mt.easytv.commands.CommandArgument;
import com.mt.easytv.commands.CommandArgumentList;
import com.mt.easytv.interaction.Messager;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Client
{
    private Socket                   _socket;
    private BufferedWriter           _writer;
    private XMLEncoder               _encoder;
    private XMLDecoder               _decoder;
    private BufferedReader           _reader;
    private ClientMessage            _magic;
    private ClientMessage            _auth;
    private ArrayList<ClientMessage> _messages;
    private boolean _accepted        = false;
    private boolean _sentServerMagic = false;

    public Client(Socket socket) throws IOException {
        this._socket = socket;
        this._writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this._reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this._encoder = new XMLEncoder(socket.getOutputStream());
        this._decoder = new XMLDecoder(socket.getInputStream());
        this._messages = new ArrayList<>();
    }

    public boolean hasMessaged() throws IOException {
        return this._socket.getInputStream().available() > 0;
    }

    public ClientMessage getLatestMessage() throws InvalidMessageException {
        Object obj = this._decoder.readObject();

        ClientMessage message = new ClientMessage();
        if (obj.getClass().isInstance(message)) {
            throw new InvalidMessageException(obj, message.getClass().getName());
        }

        message = (ClientMessage) obj;
        this._messages.add(message);
        return message;
    }

    public ClientMessage getPreviousMessage() {
        return this._messages.get(0);
    }

    public void message(String message) throws IOException {
        this._writer.write(message);
    }

    public void message(ServerMessage message) throws IOException {
        this._encoder.writeObject(message);
    }

    public boolean checkAuth() throws IOException, InvalidMessageException {
        if (!this.checkMagic() || (!this.hasMessaged() && this._auth == null)) {
            return false;
        }
        else if (this._auth == null) {
            this._auth = this.getLatestMessage();
        }

        this._auth.loadCommandArguments();
        CommandArgumentList args = this._auth.getCommandArguments();

        return Main.config.getValue("username").equals(args.getValue("username")) &&
               Main.config.getValue("password").equals(args.getValue("password"));
    }

    public boolean checkMagic() throws IOException, InvalidMessageException {
        if (!this.hasMessaged() && this._magic == null) {
            return false;
        }
        else if (this._magic == null) {
            this._magic = this.getLatestMessage();
        }

        return this._magic.request.equals(Main.config.getValue("serverMagic"));
    }

    public boolean hasSentMagic() {
        return this._magic != null;
    }

    public boolean hasSentAuth() {
        return this._auth != null;
    }

    public void sendServerMagic() throws IOException {
        if (this._sentServerMagic) {
            return;
        }

        this._sentServerMagic = true;
        this.message(this._magic.buildReply(true, "Magic correct", new CommandArgument("magic", Main.config.getValue("clientMagic"))));
    }

    public void sendAcceptedAuth() throws IOException {
        this.message(this._auth.buildReply(true, "Accepted auth"));
    }

    public void acceptConnection() throws IOException {
        if (this._accepted) {
            Messager.immediateMessage("Warning, acceptConnection called twice");
            return;
        }

        this._accepted = true;
        this.message(this._auth.buildReply(true, "Accepted auth"));
    }

    public void denyConnection() throws IOException {
        this._accepted = false;
        this.message(this._auth.buildReply(false, "Invalid auth"));
        this.closeConnection();
    }

    public void closeConnection() throws IOException {
        this._writer.close();
        this._reader.close();
        this._socket.shutdownInput();
        this._socket.shutdownOutput();
        this._socket.close();
    }

    public boolean isAccepted() {
        return this._accepted;
    }
}
package com.mt.easytv.connection;

import com.mt.easytv.Main;
import com.mt.easytv.commands.ArgumentNotFoundException;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.commands.CommandNotFoundException;
import com.mt.easytv.interaction.Message;
import com.mt.easytv.interaction.Messager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;

public class Server extends Thread
{
    private ArrayList<Client> _clients = new ArrayList<>();
    private ServerSocket _listener;
    private boolean _listening = false;

    public Server() throws IOException {
        this._initListener();
    }

    @Override
    public void run() {
        Messager.message("Listener server thread started");
    }

    public void checkWaitingClients() throws IOException {
        for (Client client : this.getWaitingClients()) {
            if (client.checkMagic()) {
                client.acceptConnection();
            }
        }
    }

    public void processAllCommands(CommandHandler handler) throws CommandNotFoundException, ArgumentNotFoundException, IOException {
        for (Client client : this.getActiveClients()) {
            Message message = new Message(false);
            Messager.attachMessage(message);

            try {
                message.data = handler.processCommand(client.getLatestMessage(), client); //all errors use message and message will relay to client
                message.success = true;
            }
            catch (Exception e) {
                Messager.error("", e);
            }
            finally {
                Messager.detachMessage();
            }

            try {
                client.message(message);
            }
            catch (IOException e) {
                Messager.error("Error messaging client ", e);
            }
        }
    }

    public void startListening() throws IOException {
        if (this._listener.isClosed()) {
            this._initListener();
        }

        Messager.message("listener server started");
        while (this._listening) {
            try {
                this._clients.add(new Client(this._listener.accept()));
            }
            catch (SocketException e) {
                Messager.message("Stopped listening while awaiting client.");
            }
        }
    }

    public boolean isListening() {
        return this._listening;
    }

    public void stopListening() throws IOException {
        this._listening = false;
        this._listener.close();
    }

    public ArrayList<Client> getClients() {
        return this._clients;
    }

    public ArrayList<Client> getActiveClients() {
        ArrayList<Client> activeClients = new ArrayList<>();

        for (Client client : this._clients) {
            if (!client.isWaiting()) {
                activeClients.add(client);
            }
        }
        return activeClients;
    }

    public ArrayList<Client> getWaitingClients() {
        ArrayList<Client> waitingClients = new ArrayList<>();

        for (Client client : this._clients) {
            if (client.isWaiting()) {
                waitingClients.add(client);
            }
        }
        return waitingClients;
    }

    private void _initListener() throws IOException {
        int port = Integer.parseInt(Main.config.getValue("port"));
        this._listener = new ServerSocket(port);
    }
}
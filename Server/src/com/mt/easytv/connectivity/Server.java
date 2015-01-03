package com.mt.easytv.connectivity;

import com.mt.easytv.Main;
import com.mt.easytv.commands.ArgumentNotFoundException;
import com.mt.easytv.commands.CommandHandler;
import com.mt.easytv.commands.CommandNotFoundException;
import com.mt.easytv.interaction.Message;
import com.mt.easytv.interaction.Messager;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class Server
{
    private ArrayList<Client> _clients = new ArrayList<>();
    private ServerSocket _listener;
    private Thread       _listenerThread;
    private boolean _listening = false;

    public Server() throws IOException {
        this._initListener();
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
            if (!client.hasMessaged()) {
                continue;
            }

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
        /* Setting up the listener */
        if (this._listenerThread != null && this._listenerThread.isAlive() && !this._listenerThread.isInterrupted()) {
            Messager.message("Warning, startListening called when listener thread is still alive");
            this.stopListening();
        }
        if (this._listener == null || this._listener.isClosed()) {
            this._initListener();
        }

        /* Start listening on a different thread to stop blocking */
        this._listenerThread = new Thread(() ->
                                          {
                                              Messager.message("Listener server started");
                                              this._listening = true;

                                              while (this._listening) {
                                                  try {
                                                      this._clients.add(new Client(this._listener.accept()));
                                                  }
                                                  catch (IOException e) {
                                                      if (this._listening) {
                                                          Messager.error("Stopped listening while awaiting client ", e);
                                                      }
                                                  }
                                              }

                                              Messager.message("Listener server stopped");
                                          });
        this._listenerThread.start();
    }

    public boolean isListening() {
        return this._listening;
    }

    public void stopListening() throws IOException {
        this._listening = false;

        if (this._listener != null) {
            this._listener.close();
        }

        if (this._listenerThread != null) {
            this._listenerThread.interrupt();
        }
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
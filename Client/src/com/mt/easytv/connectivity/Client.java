package com.mt.easytv.connectivity;

import com.mt.easytv.activities.DebugManager;
import com.mt.easytv.config.Config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Client
{
    private static Server                   _server;
    private static ArrayList<ClientMessage> _pendingMessages;
    private static ArrayList<ClientMessage> _messages;
    private static ArrayList<ServerMessage> _responses;
    private static ConnectionStatus _status = ConnectionStatus.NOT_CONNECTED;

    public static void startup() throws Exception {
        String restrictHost = Config.getValue("restrictIp");
        InetAddress[] possibleServers;

        if (restrictHost != null) {
            possibleServers = new InetAddress[]{InetAddress.getByName(restrictHost)};
        }
        else {
            possibleServers = Client.getPossibleServers();
        }

        if (possibleServers.length == 0) {
            throw new Exception("No possible servers found.");
        }

        Client.attemptConnection(possibleServers);
    }

    public static void addToQue(ClientMessage message) throws Exception {
        if (Client._pendingMessages.contains(message)) {
            throw new Exception("Message already in que");
        }

        Client._pendingMessages.add(message);
    }

    public static boolean removeFromQue(ClientMessage message) {
        return Client._pendingMessages.remove(message);
    }

    public static InetAddress[] getPossibleServers() throws Exception {
        InetAddress local;

        try {
            local = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e) {
            DebugManager.handleException(e);
            throw new Exception("Local host not found. Please check your network connection.");
        }

        ArrayList<InetAddress> addresses = new ArrayList<>();

        String localAddress = local.toString().substring(0, local.toString().lastIndexOf(".") + 1);

        for (int range = 0; range < 255; range++) {
            try {
                InetAddress possibleAddress = InetAddress.getByName(localAddress + range);

                if (possibleAddress.isReachable(Integer.parseInt(Config.getValue("connectionTimeout")))) {
                    addresses.add(possibleAddress);
                }

                DebugManager.log("Found host " + possibleAddress.toString());
            }
            catch (UnknownHostException e) {
                //not a host on this network
            }
            catch (IOException e) {
                DebugManager.handleException(e);
            }
        }

        if (addresses.size() == 0) {
            return new InetAddress[0];
        }

        return addresses.toArray(new InetAddress[addresses.size() - 1]);
    }

    public static void attemptConnection(InetAddress address) throws IOException {
        DebugManager.log("Attempting connection to " + address.toString());
        Client._status = ConnectionStatus.CONNECTING;
        Client._server = new Server(address);
        Client._status = ConnectionStatus.CONNECTED;
        Client._server.attemptAuth();
        Client._status = ConnectionStatus.AUTHENTICATED;

        //TODO: start the loop that will send pending messages and move them into regular messages array as well as handling incoming messages
        Client._messages = new ArrayList<>();
        Client._pendingMessages = new ArrayList<>();
        Client._responses = new ArrayList<>();
    }

    public static void attemptConnection(InetAddress[] addresses) {
        for (InetAddress address : addresses) {
            try {
                Client.attemptConnection(address); //TODO: fix this, will not work as auth will be async

                if (Client._status == ConnectionStatus.READY) {
                    break;
                }
            }
            catch (IOException e) {
                //not able to connect, try the next one
            }
        }
    }

    public static ConnectionStatus getStatus() {
        return Client._status;
    }
}
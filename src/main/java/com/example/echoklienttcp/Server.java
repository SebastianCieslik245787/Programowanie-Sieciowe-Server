package com.example.echoklienttcp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Objects;

public class Server {
    private Boolean isActive = false;
    private Integer clientCounter = 0;
    private Integer activeClients = 0;
    private final Integer SERVER_CAPACITY = 3;

    ServerWindowController serverWindowController;

    private final Integer serverPort;
    String SERVER_ADDRESS = "0.0.0.0";

    private ServerSocket serverSocket = null;

    private Client client;
    private final HashMap<Integer, Client> clients = new HashMap<>();

    public Server(Integer serverPort, ServerWindowController serverWindowController) {
        this.serverPort = serverPort;
        this.serverWindowController = serverWindowController;
    }

    public Integer getServerPort(){
        return serverPort;
    }

    public boolean startServer(){
        try{
            serverSocket = new ServerSocket(serverPort, SERVER_CAPACITY, InetAddress.getByName(SERVER_ADDRESS));
            int SERVER_TIME_OUT = 1000;
            serverSocket.setSoTimeout(SERVER_TIME_OUT);
            isActive = true;
            serverWindowController.logMessage(" Server started at port: " + serverPort + " with address: " + SERVER_ADDRESS);
        }
        catch(IOException e){
            return false;
        }
        return true;
    }

    public void listenForClients() {
        while (isActive) {
            serverWindowController.setActiveClientsFiled(getInfoAboutClients());
            try {
                Socket clientSocket = serverSocket.accept();
                if((Objects.equals(activeClients, SERVER_CAPACITY))){
                    serverWindowController.logMessage(" Client with address: " + clientSocket.getInetAddress().getHostAddress() + " tried to connect but server is busy!");
                    DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
                    outToClient.writeUTF("Server is busy");
                    clientSocket.close();
                    continue;
                }
                client = new Client(clientSocket, serverWindowController, clientCounter, this);
                clients.put(client.getId(), client);
                activeClients++;
                clientCounter++;
                serverWindowController.setActiveClientsCounter(activeClients);
                Thread clientThread = new Thread(() -> {
                    try {
                        client.initialize();
                        client.listenForMessage();
                    } catch (IOException e) {
                        disconnectClient(client.getId());
                    }
                });
                clientThread.start();
            } catch (IOException _) {}
        }
    }

    public boolean stopServer(){
        isActive = false;
        if(activeClients > 0){
            for(Client c : clients.values()){
                if(c.closeConnectionWithServer()) return false;
                activeClients--;
            }
            clients.clear();
        }
        try {
            serverSocket.close();
            serverWindowController.logMessage(" Server stopped!");
        }
        catch(IOException e){
            serverWindowController.logMessage(" Could not stop server!");
            return false;
        }
        return true;
    }

    public void disconnectClient(int id){
        if(clients.get(id).closeConnectionWithServer()) return;
        activeClients--;
        clients.remove(id);
        serverWindowController.setActiveClientsFiled(getInfoAboutClients());
        serverWindowController.setActiveClientsCounter(activeClients);
        listenForClients();
    }

    public String getInfoAboutClients(){
        StringBuilder message = new StringBuilder();
        for(Client c : clients.values()){
            message.append(c.getClientInfo()).append("\n");
        }
        return message.toString();
    }
}

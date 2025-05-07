package com.example.echoklienttcp;

import java.io.IOException;
import java.net.*;

public class Server {
    private Boolean isActive = false;
    private Integer clientCounter = 0;
    private Integer activeClients = 0;
    private final Integer SERVER_CAPACITY = 1;

    ServerWindowController serverWindowController;

    private final Integer serverPort;
    String SERVER_ADDRESS = "0.0.0.0";

    private ServerSocket serverSocket = null;

    private Client client;

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
        while ((activeClients < SERVER_CAPACITY) && isActive) {
            System.out.println("1");
            try {
                client = new Client(serverSocket.accept(), serverWindowController, clientCounter, this);
                activeClients++;
                clientCounter++;
                serverWindowController.setActiveClientsCounter(activeClients);
                Thread clientThread = new Thread(() -> {
                    try {
                        client.initialize();
                        client.listenForMessage();
                    } catch (IOException e) {
                        disconnectClient();
                    }
                });
                clientThread.start();
            } catch (IOException _) {}
        }
    }

    public boolean stopServer(){
        isActive = false;
        if(activeClients > 0){
            if(client.closeConnectionWithServer()) return false;
            activeClients--;
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

    public void disconnectClient(){
        if(client.closeConnectionWithServer()) return;
        activeClients--;
        serverWindowController.setActiveClientsCounter(activeClients);
        listenForClients();
    }
}

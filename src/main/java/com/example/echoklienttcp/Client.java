package com.example.echoklienttcp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
    private final Integer id;
    private Boolean isActive = false;
    private final Server server;

    private final Socket clientSocket;
    private BufferedReader inFromClient = null;
    private DataOutputStream outToClient = null;
    private String clientAddress = "";
    private final ServerWindowController serverWindowController;

    public Client(Socket clientSocket, ServerWindowController serverWindowController, Integer id, Server server) {
        this.server = server;
        this.clientSocket = clientSocket;
        this.serverWindowController = serverWindowController;
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void initialize() throws IOException {
        this.inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.outToClient = new DataOutputStream(clientSocket.getOutputStream());
        this.clientAddress = clientSocket.getInetAddress().getHostAddress();
        isActive = true;
        serverWindowController.logMessage(" Client connected with number: #" + id + " and address: " + clientAddress);
        listenForMessage();
    }

    public boolean closeConnectionWithServer() {
        try {
            clientSocket.close();
            serverWindowController.logMessage(getClientInfo() + " has disconnected!");
            isActive = false;
        } catch (IOException e) {
            serverWindowController.logMessage(" Client can not disconnect: " + e.getMessage());
            return true;
        }
        return false;
    }

    public String getClientInfo() {
        return "[#" + id + " | " + clientAddress + ":" + server.getServerPort() + "]";
    }

    void listenForMessage() {
        while (isActive) {
            try {
                char[] buffer = new char[1024];
                int bytesRead = inFromClient.read(buffer);
                if (bytesRead == -1) {
                    server.disconnectClient(id);
                    isActive = false;
                    continue;
                }
                String clientMessage = new String(buffer, 0, bytesRead);
                if (clientMessage.getBytes().length > 1024) {
                    serverWindowController.logMessage(getClientInfo() + " Client has send too big message: (" + clientMessage.getBytes().length + " bytes)");
                    sendMessageToClient("Your message was too big: (Message size: " + clientMessage.getBytes().length + " bytes | Max size: 1024 bytes)");
                } else {
                    serverWindowController.logMessage(getClientInfo() + " Client has send a message: \"" + clientMessage + "\" (" + clientMessage.getBytes().length + " bytes)");
                    sendMessageToClient(clientMessage);
                }

            } catch (IOException e) {
                server.disconnectClient(id);
            }
        }
    }

    private void sendMessageToClient(String message) throws IOException {
        try {
            outToClient.writeBytes(message);
            serverWindowController.logMessage(getClientInfo() + " Message: \"" + message + "\" has sent to client! (" + message.getBytes().length + " bytes)");
        } catch (IOException e) {
            serverWindowController.logMessage(getClientInfo() + " Could not send message to client!");
        }
    }
}
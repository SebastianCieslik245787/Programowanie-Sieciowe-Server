package com.example.echoklienttcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    private Socket clientSocket;
    private DataOutputStream outToClient;
    private DataInputStream inFromClient;
    private InetAddress clientAddress;
    private int clientPort;

    public Client(Socket clientSocket, InetAddress clientAddress, int clientPort, DataOutputStream outToClient, DataInputStream inFromClient) {
        this.clientSocket = clientSocket;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.outToClient = outToClient;
        this.inFromClient = inFromClient;
    }
}

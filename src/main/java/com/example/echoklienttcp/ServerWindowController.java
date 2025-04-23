package com.example.echoklienttcp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Paint;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ServerWindowController {
    @FXML
    public TextArea portField;
    @FXML
    public TextArea serverLogsField;
    @FXML
    public Button buttonStop;
    @FXML
    public Button buttonStart;
    @FXML
    public Label serverStatus;
    @FXML
    public Label clientsCounter;
    @FXML
    public Label errorLabel;

    private ServerSocket serverSocket = null;
    private Socket[] clientSocket;
    private BufferedReader[] inFromClient;
    private DataOutputStream[] outToClient;
    private Integer[] clientPort;
    private String[] clientAddress;
    private boolean[] queue = {false, false, false};
    private Thread[] clientThread = new Thread[3];

    private Boolean isActive = false;
    private Integer clientCounter = 0;
    private Integer serverPort = 7;
    private final String SERVER_ADDRESS = "0.0.0.0";
    private Integer connectedClientsCoutner = 0;
    /*
     * Użyłem osobnego wątku dla serwera żeby móc dynamicznie wprowadzać zmiany w aplikacji, ponieważ nie mogłem znaleśźć innego sensownego rozwiązania
     * https://stackoverflow.com/questions/55597189/how-to-update-my-javafx-gui-elements-while-program-is-running
     */
    private Thread serverThread = null;

    private final Integer SERVER_TIME_OUT = 1000;

    public void initialize() {
        buttonStart.setOnAction(_ -> {
            if (validatePort()) {
                try {
                    logMessage(getTime() + " Server started at port: " + portField.getText() + " with address: " + SERVER_ADDRESS);
                    toggleConnectionUI(true);
                    setServerStatusUI(true);
                    isActive = true;
                    serverSocket = new ServerSocket(serverPort, 1, InetAddress.getByName(SERVER_ADDRESS));
                    serverSocket.setSoTimeout(SERVER_TIME_OUT);
                } catch (IOException e) {
                    logMessage(getTime() + " Could not start at port: " + portField.getText());
                }
                serverThread = new Thread(() -> {
                    try {
                        listenForClients();
                    } catch (IOException | InterruptedException e) {
                        logMessage(getTime() + " Server error: " + e.getMessage());
                    }
                });
                serverThread.start();
            }
        });

        buttonStop.setOnAction(_ -> {
            try {
                isActive = false;
                serverThread.join();
                if (isClientConnected) {
                    clientSocket.close();
                    logMessage(getTime() + getClientInfo() + " Client disconnected!");
                }
                isClientConnected = false;
                serverSocket.close();
                logMessage(getTime() + " Server stopped!");
                toggleConnectionUI(false);
                setServerStatusUI(false);
            } catch (IOException e) {
                logMessage(getTime() + " Could not stop server!");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
    /*
     * po wprowadzeniu servera jako osobny wątek wyskakiwał error o tym że probuje modyfikować elementy JavaFX w innym wątku niż wątku aplikacji
     * znalazłem żeby użyć Platform.runLater na https://www.reddit.com/r/javahelp/comments/7qvqau/problem_with_updating_gui_javafx/
     */
    private void logMessage(String message) {
        Platform.runLater(() -> serverLogsField.appendText(message + "\n"));
    }

    private String getTime() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return "[" + date.format(formatter) + "]";
    }

    private void toggleConnectionUI(Boolean serverStatus) {
        portField.setEditable(!serverStatus);

        buttonStart.setDisable(serverStatus);
        buttonStop.setDisable(!serverStatus);

        buttonStart.setVisible(!serverStatus);
        buttonStop.setVisible(serverStatus);
    }

    private Boolean validatePort() {
        if (portField.getText().isEmpty()) {
            errorLabel.setText("Port can not be empty");
            errorLabel.setVisible(true);
            return false;
        }

        String PORT_PATTERN = "^(6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d|6[0-4]\\d\\d\\d|[0-5]?\\d?\\d?\\d?\\d)$";

        if (!portField.getText().matches(PORT_PATTERN)) {
            errorLabel.setText("Port must be a valid port number! (Possible ports are: 0-65535)");
            errorLabel.setVisible(true);
            return false;
        }

        serverPort = Integer.parseInt(portField.getText());
        errorLabel.setVisible(false);
        return true;
    }

    private void setServerStatusUI(boolean connectionStatus) {
        if (connectionStatus) {
            serverStatus.setText("Active");
            serverStatus.setTextFill(Paint.valueOf("#058e2e"));
        } else {
            serverStatus.setText("Inactive");
            serverStatus.setTextFill(Paint.valueOf("#e10202"));
        }
    }

    private void listenForClients() throws IOException, InterruptedException {
        while (isActive) {
            try {
                Socket tempClientSocket = serverSocket.accept();
                if(connectedClientsCoutner > 2){
                    logMessage(getTime() + " Client can not connect queue is full!");
                    DataOutputStream tempOutToClient = new DataOutputStream(tempClientSocket.getOutputStream());
                    tempOutToClient.writeBytes("Server is busy!" + "\n");
                    tempClientSocket.close();
                }
                else{
                    BufferedReader tempInFromClient = new BufferedReader(new InputStreamReader(tempClientSocket.getInputStream()));
                    DataOutputStream tempOutToClient = new DataOutputStream(tempClientSocket.getOutputStream());
                    String tempClientAddress = tempClientSocket.getInetAddress().getHostAddress();
                    int tempClientPort = tempClientSocket.getPort();
                    setNewClient(tempClientSocket, tempClientAddress, tempClientPort, tempOutToClient, tempInFromClient);
                }
            } catch (IOException _) {}
        }
    }

    private void setNewClient(Socket clientSocket, String clientAddress, int clientPort, DataOutputStream outToClient, BufferedReader inFromClient) throws IOException, InterruptedException {
        int index = 0;
        for (int i = 0; i < 3;i++){
            if(!queue[i]){
                index = i;
                break;
            }
        }
        this.clientSocket[index] = clientSocket;
        this.inFromClient[index] = inFromClient;
        this.outToClient[index] = outToClient;
        this.clientAddress[index] = clientAddress;
        this.clientPort[index] = clientPort;
        connectedClientsCoutner++;
        setClientsCounter();
        logMessage(getTime() + " Client connected with number: #" + index + " and address: " + clientAddress + " and port: " + clientPort);
        clientThread[index] = new Thread(() -> {
            listenForClientMessage(index);
        });
    }

    private void listenForClientMessage(int index) throws IOException, InterruptedException {
        while (isActive) {
            if(inFromClient[index].ready()) {
                try {
                    String clientMessage = inFromClient[index].readLine();
                    if (clientMessage == null) {
                        logMessage(getTime() + getClientInfo() + " Client disconnected!");
                        listenForClients();
                    } else if (clientMessage.getBytes().length > 1024) {
                        logMessage(getTime() + getClientInfo() + " Client has send too big message: (" + clientMessage.getBytes().length + " bytes)");
                        sendMessageToClient("Your message was too big: (Message size: " + clientMessage.getBytes().length + " bytes | Max size: 1024 bytes)", index);
                    } else {
                        logMessage(getTime() + getClientInfo() + " Client has send a message: \"" + clientMessage + "\" (" + clientMessage.getBytes().length + " bytes)");
                        sendMessageToClient(clientMessage, index);
                    }

                } catch (IOException e) {
                    logMessage(getTime() + getClientInfo() + " Client disconnected!");
                    listenForClients();
                }
            }
        }
    }

    private void setClientsCounter() {
        clientCounter++;
        Platform.runLater(() -> clientsCounter.setText(String.valueOf(clientCounter)));
    }

    private void sendMessageToClient(String message, int index) throws IOException {
        try {
            outToClient[index].writeBytes(message + "\n");
            logMessage(getTime() + getClientInfo() + " Message: \"" + message + "\" has sent to client! (" + message.getBytes().length + " bytes)");
        } catch (IOException e) {
            logMessage(getTime() + getClientInfo() + " Could not send message to client!");
        }
    }

    private String getClientInfo() {
        return "[#" + clientCounter + " | " + clientAddress + ":" + clientPort  + "]";
    }
}

package com.example.echoklienttcp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Paint;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    Server server;

    /*
     * Użyłem osobnego wątku dla serwera żeby móc dynamicznie wprowadzać zmiany w aplikacji, poniewarz nie mogłem znaleść innego sensownego rozwiązania
     * https://stackoverflow.com/questions/55597189/how-to-update-my-javafx-gui-elements-while-program-is-running
     */
    private Thread serverThread = null;

    public void initialize() {
        buttonStart.setOnAction(_ -> {
            if (validatePort()) {
                server = new Server(Integer.parseInt(portField.getText()), this);
                if(server.startServer()){
                    toggleConnectionUI(true);
                    setServerStatusUI(true);
                }
                else{
                    logMessage(" Could not start at port: " + portField.getText());
                }
                serverThread = new Thread(server::listenForClients);
                serverThread.start();
            }
        });

        buttonStop.setOnAction(_ -> {
            try {
                if(!server.stopServer()) return;
                serverThread.join();
                toggleConnectionUI(false);
                setServerStatusUI(false);
            }catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
    /*
     * po wprowadzeniu servera jako osobny wątek wyskakiwał error o tym że probuje modyfikować elementy JavaFX w innym wątku niż wątku aplikacji
     * znalazłem żeby użyć Platform.runLater na https://www.reddit.com/r/javahelp/comments/7qvqau/problem_with_updating_gui_javafx/
     */
    public void logMessage(String message) {
        Platform.runLater(() -> serverLogsField.appendText(getTime() + message + "\n"));
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

    public void setActiveClientsCounter(int activeClients) {
        Platform.runLater(() -> clientsCounter.setText(String.valueOf(activeClients)));
    }
}

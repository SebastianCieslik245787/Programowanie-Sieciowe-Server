package com.example.echoklienttcp;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class Server extends Application {
    @FXML
    public  TextArea portField;
    @FXML
    public  TextArea serverLogsField;
    @FXML
    public ImageView onOffButton;

    private static Boolean onOff = false;
    private static ServerSocket serverSocket;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Server.class.getResource("serverWindow.fxml"));
        fxmlLoader.setController(this);
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Server");
        stage.setScene(scene);
        stage.show();

        onOffButton.setOnMouseClicked(event -> {
            portField.setEditable(!onOff);

            if (!onOff) {
                onOffButton.setImage(new Image(Objects.requireNonNull(getClass().getResource("/images/power_off.png")).toExternalForm()));
                onOff = true;
                try {
                    startServer();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                onOffButton.setImage(new Image(Objects.requireNonNull(getClass().getResource("/images/power_on.png")).toExternalForm()));
                onOff = false;
                stopServer();
            }
        });
    }

    public void startServer() throws IOException {
        int port = Integer.parseInt(portField.getText());

        try{
            serverSocket = new ServerSocket(port);
            serverLogsField.appendText("Server started on PORT: " + port + "\n");
        }
        catch (Exception e){
            serverLogsField.appendText("Server can not start on PORT: " + port + "\n");
            onOff = false;
            return;
        }

        new Thread(() -> {
            try {
                Socket socket = serverSocket.accept();
                serverLogsField.appendText("Client with Address: " + socket.getInetAddress() + " has connected to the Server\n");

                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

                String message = "";
                while (onOff && (input.read()) != -1) {
                    serverLogsField.appendText("Client sent: " + message + "\n");
                    output.println(message);
                }
                serverLogsField.appendText("Client sent: " + message + "\n");
                output.println(message);
                serverLogsField.appendText("Client disconnected.\n");
                socket.close();
            } catch (Exception ignored) {}
        }).start();
    }

    public void stopServer() {
        onOff = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                serverLogsField.appendText("Server stopped.\n");
            } catch (IOException e) {
                serverLogsField.appendText("Error stopping server.\n");
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
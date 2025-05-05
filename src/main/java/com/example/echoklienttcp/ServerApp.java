package com.example.echoklienttcp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class ServerApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ServerApp.class.getResource("ServerWindow.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Server TCP");
        stage.setScene(scene);
        stage.show();
        ServerWindowController controller = fxmlLoader.getController();
        controller.initialize();
    }

    public static void main(String[] args) {
        launch();
    }
}
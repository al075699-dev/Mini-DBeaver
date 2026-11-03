package main;

import database.ConexionRemota;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public void start(Stage escenario) throws IOException {

        Parent root = FXMLLoader.load(getClass().getResource("/PantallaLogin.fxml"));
        Scene escena = new Scene(root);

        escenario.setScene(escena);

        escenario.show();
    }

    public static void main (String[] args) {launch(args);}
}

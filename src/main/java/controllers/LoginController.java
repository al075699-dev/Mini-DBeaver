package controllers;

import com.jcraft.jsch.JSchException;
import database.ConexionRemota;
import database.ConsultasRemotas;
import database.SqliteService;
import database.SshTunnel;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


import java.sql.SQLException;

public class LoginController {

    @FXML private TextField txtSshHost;
    @FXML private TextField txtSshUsuario;
    @FXML private PasswordField txtSshPassword;
    @FXML private TextField txtDbHost;
    @FXML private TextField txtDbUsuario;
    @FXML private PasswordField txtDbPassword;
    @FXML private Button btnConectar;

    @FXML
    private void conectar() {
        String sshHost = txtSshHost.getText().trim();
        String sshUser = txtSshUsuario.getText().trim();
        String sshPass = txtSshPassword.getText();
        String dbHost = txtDbHost.getText().trim();
        String dbUser = txtDbUsuario.getText().trim();
        String dbPass = txtDbPassword.getText();

        if (sshHost.isEmpty() || sshUser.isEmpty() || sshPass.isEmpty()
                || dbHost.isEmpty() || dbUser.isEmpty() || dbPass.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos",
                    "Llena todos los campos antes de conectar.");
            return;
        }

        btnConectar.setDisable(true);

        Task<Sesion> tarea = new Task<>() {
            @Override
            protected Sesion call() throws Exception {
                Sesion s = new Sesion();
                try {
                    s.tunel = new SshTunnel();
                    s.tunel.abrir(sshHost, sshUser, sshPass, dbHost);

                    s.remota = new ConexionRemota();
                    s.remota.conectar(s.tunel.getLocalPort(), dbUser, dbPass);

                    s.consultas = new ConsultasRemotas(s.remota);

                    s.sqlite = new SqliteService("datos_locales.db");
                    s.sqlite.conectar();
                    return s;
                } catch (Exception e) {
                    s.cerrar();   // no dejar el túnel abierto si algo fallo
                    throw e;
                }
            }
        };

        tarea.setOnSucceeded(e -> abrirPrincipal(tarea.getValue()));

        tarea.setOnFailed(e -> {
            btnConectar.setDisable(false);
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo conectar",
                    describirError(tarea.getException()));
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void abrirPrincipal(Sesion sesion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Descargas.fxml"));
            Parent root = loader.load();

            PrincipalController controlador = loader.getController();
            controlador.init(sesion);

            Stage stage = (Stage) btnConectar.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 550));
            stage.setOnCloseRequest(ev -> sesion.cerrar());
        } catch (Exception ex) {
            sesion.cerrar();
            btnConectar.setDisable(false);
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo abrir la pantalla principal: " + ex.getMessage());
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        alerta.initOwner(btnConectar.getScene().getWindow());
        alerta.showAndWait();
    }

    private String describirError(Throwable ex) {
        String m = String.valueOf(ex.getMessage());

        if (ex instanceof JSchException) {
            if (m.contains("Auth fail") || m.contains("Auth cancel"))
                return "Usuario o contraseña de SSH incorrectos.";
            if (m.contains("UnknownHostException"))
                return "No se encontró el host SSH. Revisa el nombre y tu conexión a internet.";
            if (m.contains("timeout") || m.contains("timed out"))
                return "El servidor SSH no respondió. Revisa tu red.";
            if (m.contains("refused"))
                return "El servidor rechazó la conexión SSH.";
            return "Error de SSH: " + m;
        }

        if (ex instanceof SQLException sql) {
            if (sql.getErrorCode() == 1045)
                return "Usuario o contraseña de MariaDB incorrectos.";
            if (sql.getErrorCode() == 1049)
                return "La base de datos its5a no existe en el servidor.";
            return "Error de MariaDB: " + m;
        }

        return "Error inesperado: " + m;
    }
}
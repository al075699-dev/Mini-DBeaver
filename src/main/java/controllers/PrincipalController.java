package controllers;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import model.Registro;
import model.Tabla;

import java.sql.SQLException;
import java.util.List;

public class PrincipalController {

    @FXML private ListView<String> listaTablas;
    @FXML private Button btnDescargar;
    @FXML private Button btnVerDescargadas;
    @FXML private Button btnSalir;
    @FXML private Label lblEstado;

    private final BooleanProperty descargando = new SimpleBooleanProperty(false);
    private Sesion sesion;

    public void init(Sesion sesion) throws SQLException {
        this.sesion = sesion;
        lblEstado.setText("");
        listaTablas.getItems().setAll(sesion.consultas.listarTablas());

        // Durante una descarga se bloquea todo
        listaTablas.disableProperty().bind(descargando);
        btnVerDescargadas.disableProperty().bind(descargando);
        btnSalir.disableProperty().bind(descargando);

        // "Descargar" solo se activa con una tabla seleccionada y sin descarga en curso
        btnDescargar.disableProperty().bind(
                listaTablas.getSelectionModel().selectedItemProperty().isNull().or(descargando));
    }

    // ---------- Descargar ----------

    @FXML
    private void descargar() {
        String nombre = listaTablas.getSelectionModel().getSelectedItem();
        if (nombre == null) return;

        try {
            if (sesion.sqlite.existeTabla(nombre)) {
                ButtonType redescargar = new ButtonType("Descargar de nuevo");
                Alert aviso = new Alert(Alert.AlertType.CONFIRMATION,
                        "Ya descargaste «" + nombre + "». Si la descargas de nuevo, "
                                + "se perderán los cambios hechos en tu copia local.",
                        redescargar, ButtonType.CANCEL);
                aviso.setHeaderText("Tabla ya descargada");
                aviso.initOwner(btnSalir.getScene().getWindow());

                if (aviso.showAndWait().orElse(ButtonType.CANCEL) != redescargar) return;
            }
        } catch (SQLException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", ex.getMessage());
            return;
        }

        iniciarDescarga(nombre);
    }

    private void iniciarDescarga(String nombre) {
        descargando.set(true);
        lblEstado.setText("Descargando «" + nombre + "»...");

        Task<Tabla> tarea = new Task<>() {
            @Override
            protected Tabla call() throws Exception {
                Tabla tabla = sesion.consultas.obtenerTabla(nombre);
                List<Registro> registros = sesion.consultas.leerRegistros(tabla);
                sesion.sqlite.guardarTabla(tabla, registros);
                return tabla;
            }
        };

        tarea.setOnSucceeded(e -> {
            descargando.set(false);
            lblEstado.setText("«" + nombre + "» descargada. Pulsa «Ver descargadas» para trabajar con ella.");
        });

        tarea.setOnFailed(e -> {
            descargando.set(false);
            lblEstado.setText("");
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo descargar",
                    String.valueOf(tarea.getException().getMessage()));
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    // ---------- Ver descargadas (ir al CRUD) ----------

    @FXML
    public void verDescargadas(ActionEvent evento) {
        System.out.println("A: " + getClass().getResource("/view/PanelPrincipal.fxml"));
        System.out.println("B: " + getClass().getResource("/PanelPrincipal.fxml"));
        System.out.println("C: " + getClass().getResource("/controllers/PanelPrincipal.fxml"));
        System.out.println("D: " + getClass().getResource("PanelPrincipal.fxml"));
        try {
            if (sesion.sqlite.listarTablasLocales().isEmpty()) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sin tablas descargadas",
                        "Aún no has descargado ninguna tabla. Selecciona una y pulsa Descargar.");
                return;
            }

            sesion.tablaActual = null;   // en el CRUD el usuario elige la tabla de la lista

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PanelPrincipal.fxml"));
            Parent root = loader.load();
            SqlCrudController controlador = loader.getController();
            controlador.init(sesion);

            Stage stage = (Stage) btnSalir.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el CRUD: " + ex.getMessage());
        }
    }

    // ---------- Salir ----------

    @FXML
    private void salir() {
        sesion.cerrar();
        Platform.exit();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        alerta.initOwner(btnSalir.getScene().getWindow());
        alerta.showAndWait();
    }
}
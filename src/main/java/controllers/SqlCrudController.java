package controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import model.Columnas;
import model.Registro;
import model.ResultadoSql;
import model.Tabla;

import java.sql.SQLException;
import java.util.List;

public class SqlCrudController {

    @FXML private ListView<String> listaTablas;
    @FXML private TextArea txtEditorSql;
    @FXML private TableView<Registro> tablaResultados;
    @FXML private Button btnAgregar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnEjecutar;
    @FXML private Button btnSalir;
    @FXML private Label lblEstado;

    private Sesion sesion;
    private Tabla tablaActual;



    public void init(Sesion sesion) throws SQLException {
        this.sesion = sesion;
        txtEditorSql.clear();
        tablaResultados.getColumns().clear();
        tablaResultados.getItems().clear();
        setCrudHabilitado(false);

        refrescarLista();

        // Para abrir la tabla seleccionada
        listaTablas.setOnMouseClicked(e -> {
            String nombre = listaTablas.getSelectionModel().getSelectedItem();
            if (nombre != null) abrirTabla(nombre);
        });

        // Si venimos de descargar una tabla, se abre directamente
        if (sesion.tablaActual != null) {
            String nombre = sesion.tablaActual.obtenerNombre();
            listaTablas.getSelectionModel().select(nombre);
            abrirTabla(nombre);
        } else {
            lblEstado.setText("Selecciona una tabla de la lista.");
        }
    }



    private void refrescarLista() throws SQLException {
        listaTablas.getItems().setAll(sesion.sqlite.listarTablasLocales());
    }

    private void abrirTabla(String nombre) {
        try {
            tablaActual = sesion.sqlite.obtenerTablaLocal(nombre);
            sesion.tablaActual = tablaActual;
            txtEditorSql.setText("SELECT * FROM \"" + nombre + "\";");
            mostrarTabla();
        } catch (SQLException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo abrir la tabla", ex.getMessage());
        }
    }

    // Dibuja la tabla actual
    private void mostrarTabla() throws SQLException {
        List<String> nombres = tablaActual.obtenerColumnas().stream()
                .map(Columnas::obtenerNombre).toList();
        configurarColumnas(nombres);

        List<Registro> filas = sesion.sqlite.leerRegistros(tablaActual);
        tablaResultados.getItems().setAll(filas);

        setCrudHabilitado(true);
        lblEstado.setText("«" + tablaActual.obtenerNombre() + "»: " + filas.size() + " registros.");
    }

    private void configurarColumnas(List<String> nombres) {
        tablaResultados.getColumns().clear();
        for (String nombre : nombres) {
            TableColumn<Registro, Object> col = new TableColumn<>(nombre);
            col.setCellValueFactory(cd ->
                    new SimpleObjectProperty<>(cd.getValue().obtenerValor(nombre)));
            col.setPrefWidth(120);
            tablaResultados.getColumns().add(col);
        }
    }



    @FXML
    private void agregarRegistro() {
        if (tablaActual == null) return;
        RegistroDialog.mostrar(tablaActual, null).ifPresent(nuevo -> {
            try {
                sesion.sqlite.insertar(tablaActual, nuevo);
                mostrarTabla();
                lblEstado.setText("Registro agregado a «" + tablaActual.obtenerNombre() + "».");
            } catch (SQLException ex) {
                mostrarAlerta(Alert.AlertType.ERROR, "No se pudo agregar", ex.getMessage());
            }
        });
    }

    @FXML
    private void editarRegistro() {
        if (tablaActual == null) return;
        Registro seleccionado = tablaResultados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Editar", "Selecciona un registro primero.");
            return;
        }
        RegistroDialog.mostrar(tablaActual, seleccionado).ifPresent(nuevo -> {
            try {
                sesion.sqlite.actualizar(tablaActual, seleccionado, nuevo);
                mostrarTabla();
                lblEstado.setText("Registro actualizado.");
            } catch (SQLException ex) {
                mostrarAlerta(Alert.AlertType.ERROR, "No se pudo actualizar", ex.getMessage());
            }
        });
    }

    @FXML
    private void eliminarRegistro() {
        if (tablaActual == null) return;
        Registro seleccionado = tablaResultados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Eliminar", "Selecciona un registro primero.");
            return;
        }
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el registro seleccionado? Esta acción no se puede deshacer.");
        confirmar.setHeaderText("Eliminar registro");
        confirmar.initOwner(btnSalir.getScene().getWindow());
        if (confirmar.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            sesion.sqlite.eliminar(tablaActual, seleccionado);
            mostrarTabla();
            lblEstado.setText("Registro eliminado.");
        } catch (SQLException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo eliminar", ex.getMessage());
        }
    }

    //Editor

    @FXML
    private void ejecutarConsulta() {
        String sql = txtEditorSql.getText().trim();
        if (sql.isEmpty()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Editor SQL", "Escribe una consulta primero.");
            return;
        }
        try {
            ResultadoSql res = sesion.sqlite.ejecutarSql(sql);

            if (res.esConsulta()) {
                configurarColumnas(res.columnas());
                tablaResultados.getItems().setAll(res.filas());
                setCrudHabilitado(false);
                lblEstado.setText("Consulta ejecutada: " + res.filas().size() + " filas. "
                        + "Haz clic en una tabla para volver a editarla.");
            } else {
                refrescarLista();
                restaurarTablaActual();
                lblEstado.setText("Instrucción ejecutada. Filas afectadas: " + res.filasAfectadas());
            }
        } catch (SQLException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error en la consulta", ex.getMessage());
        }
    }

    private void restaurarTablaActual() throws SQLException {
        if (tablaActual != null && sesion.sqlite.existeTabla(tablaActual.obtenerNombre())) {
            String estado = lblEstado.getText();
            tablaActual = sesion.sqlite.obtenerTablaLocal(tablaActual.obtenerNombre());
            mostrarTabla();
            lblEstado.setText(estado);
        } else {
            tablaActual = null;
            sesion.tablaActual = null;
            tablaResultados.getColumns().clear();
            tablaResultados.getItems().clear();
            setCrudHabilitado(false);
        }
    }

    @FXML
    private void volverDescargas() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Descargas.fxml"));  // ← ver nota abajo
            Parent root = loader.load();
            PrincipalController controlador = loader.getController();
            controlador.init(sesion);

            Stage stage = (Stage) btnSalir.getScene().getWindow();
            stage.setScene(new Scene(root, 750, 550));
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo volver a descargas: " + ex.getMessage());
        }
    }

    @FXML
    private void salir() {
        sesion.cerrar();      // cierra SQLite, MariaDB y el tunel SSH
        Platform.exit();
    }



    private void setCrudHabilitado(boolean habilitado) {
        btnAgregar.setDisable(!habilitado);
        btnEditar.setDisable(!habilitado);
        btnEliminar.setDisable(!habilitado);
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
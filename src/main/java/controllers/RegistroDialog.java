package controllers;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.Columnas;
import model.Registro;
import model.Tabla;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class RegistroDialog {

    public static Optional<Registro> mostrar(Tabla tabla, Registro existente) {
        Dialog<Registro> dialogo = new Dialog<>();
        dialogo.setTitle(existente == null ? "Agregar registro" : "Editar registro");

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));

        Map<String, TextField> campos = new LinkedHashMap<>();
        int fila = 0;
        for (Columnas c : tabla.obtenerColumnas()) {
            TextField campo = new TextField();
            Object valor = existente == null ? null : existente.obtenerValor(c.obtenerNombre());
            if (valor != null) campo.setText(String.valueOf(valor));
            if (existente == null && c.esAutoIncremental()) campo.setPromptText("(automático)");

            String etiqueta = c.obtenerNombre() + " (" + c.obtenerTipo() + ")"
                    + (c.esPrimaryKey() ? " [PK]" : "");
            grid.add(new Label(etiqueta), 0, fila);
            grid.add(campo, 1, fila);
            campos.put(c.obtenerNombre(), campo);
            fila++;
        }
        dialogo.getDialogPane().setContent(grid);

        dialogo.setResultConverter(boton -> {
            if (boton != guardar) return null;
            Registro r = new Registro();
            for (Columnas c : tabla.obtenerColumnas()) {
                String texto = campos.get(c.obtenerNombre()).getText();
                boolean vacioComoNull = texto.isEmpty() && (c.esNullable() || c.esAutoIncremental());
                r.agregarValor(c.obtenerNombre(), vacioComoNull ? null : texto);
            }
            return r;
        });

        return dialogo.showAndWait();
    }
}
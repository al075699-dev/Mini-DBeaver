package database;

import model.Columnas;
import model.Registro;
import model.Tabla;

import java.sql.*;
import java.util.*;

public class ConsultasRemotas {

    private final ConexionRemota conexion;

    public ConsultasRemotas(ConexionRemota conexion) {
        this.conexion = conexion;
    }

    // Nombres de las tablas del esquema
    public List<String> listarTablas() throws SQLException {
        List<String> tablas = new ArrayList<>();
        try (Statement st = conexion.obtenerConexion().createStatement();
             ResultSet rs = st.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                tablas.add(rs.getString(1));
            }
        }
        return tablas;
    }

    // Estructura completa de una tabla: nombre + columnas
    public Tabla obtenerTabla(String nombre) throws SQLException {
        validarTabla(nombre);
        Connection con = conexion.obtenerConexion();

        //llaves primarias
        Set<String> pks = new HashSet<>();
        try (ResultSet rs = con.getMetaData().getPrimaryKeys(con.getCatalog(), null, nombre)) {
            while (rs.next()) {
                pks.add(rs.getString("COLUMN_NAME"));
            }
        }

        // metadatos de cada columna
        List<Columnas> columnas = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `" + nombre + "` LIMIT 0")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String nom = meta.getColumnName(i);
                columnas.add(new Columnas(
                        nom,
                        meta.getColumnTypeName(i),
                        meta.getPrecision(i),
                        pks.contains(nom),
                        meta.isNullable(i) != ResultSetMetaData.columnNoNulls,
                        meta.isAutoIncrement(i)
                ));
            }
        }
        return new Tabla(nombre, columnas);
    }

    // gestiona los registros
    public List<Registro> leerRegistros(Tabla tabla) throws SQLException {
        List<Registro> registros = new ArrayList<>();
        try (Statement st = conexion.obtenerConexion().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `" + tabla.obtenerNombre() + "`")) {
            while (rs.next()) {
                Registro registro = new Registro();
                for (Columnas c : tabla.obtenerColumnas()) {
                    registro.agregarValor(c.obtenerNombre(), rs.getObject(c.obtenerNombre()));
                }
                registros.add(registro);
            }
        }
        return registros;
    }

    private void validarTabla(String tabla) throws SQLException {
        if (!listarTablas().contains(tabla)) {
            throw new SQLException("La tabla no existe: " + tabla);
        }
    }
}
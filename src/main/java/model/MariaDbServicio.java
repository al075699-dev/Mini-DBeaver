package model;

import java.sql.*;
import java.util.*;

public class MariaDbServicio {

    private Connection con;

    public void conectar(int puertoLocal) throws SQLException {
        String url = "jdbc:mariadb://localhost:" + puertoLocal + "/its5a";
        con = DriverManager.getConnection(url, "becario", "FdI-its-5a");
    }

    //Lista de tablas
    public List<String> listarTablas() throws SQLException {
        List<String> tablas = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                tablas.add(rs.getString(1));
            }
        }
        return tablas;
    }


    public List<String[]> leerColumnas(String tabla) throws SQLException {
        validarTabla(tabla);
        List<String[]> columnas = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `" + tabla + "` LIMIT 0")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columnas.add(new String[] {
                        meta.getColumnName(i),
                        meta.getColumnTypeName(i)
                });
            }
        }
        return columnas;
    }

    // Registros de una tabla (cada fila es una lista de valores)
    public List<List<Object>> leerRegistros(String tabla) throws SQLException {
        validarTabla(tabla);
        List<List<Object>> filas = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `" + tabla + "`")) {
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                List<Object> fila = new ArrayList<>();
                for (int i = 1; i <= n; i++) {
                    fila.add(rs.getObject(i));
                }
                filas.add(fila);
            }
        }
        return filas;
    }

    // Sanitizacion de datos
    private void validarTabla(String tabla) throws SQLException {
        if (!listarTablas().contains(tabla)) {
            throw new SQLException("La tabla no existe: " + tabla);
        }
    }

    public void cerrar() throws SQLException {
        if (con != null && !con.isClosed()) con.close();
    }
}
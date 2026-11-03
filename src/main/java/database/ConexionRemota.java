package database;

import java.sql.*;

public class ConexionRemota {

    private Connection con;

    public void conectar(int puertoLocal, String usuario, String password) throws SQLException {
        String url = "jdbc:mariadb://localhost:" + puertoLocal + "/its5a";
        con = DriverManager.getConnection(url, usuario, password);
    }

    public void cerrar() throws SQLException {
        if (con != null && !con.isClosed()) {
            con.close();
        }
    }

    public Connection obtenerConexion() {
        return con;
    }
}
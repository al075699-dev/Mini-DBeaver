package test;

import database.ConexionRemota;
import database.ConsultasRemotas;
import database.SqliteService;
import database.SshTunnel;
import model.Columnas;
import model.Registro;
import model.Tabla;

import java.sql.*;
import java.util.List;

public class PruebaSsh {
    public static void main(String[] args) {
        SshTunnel tunel = new SshTunnel();
        ConexionRemota bd = new ConexionRemota();
        try {
            tunel.abrir("fi.jcaguilar.dev", "patito", "FdI-its-5a$$", "localhost");
            bd.conectar(tunel.getLocalPort(), "becario", "FdI-its-5a");

            ConsultasRemotas consultas = new ConsultasRemotas(bd);
            List<String> nombres = consultas.listarTablas();
            System.out.println("Tablas: " + nombres);

            Tabla tabla = consultas.obtenerTabla(nombres.get(0));
            for (Columnas c : tabla.obtenerColumnas()) {
                System.out.println(c.obtenerNombre() + " " + c.obtenerTipo() + "(" + c.obtenerTamaño() + ")"
                        + (c.esPrimaryKey() ? " PK" : "")
                        + (c.esAutoIncremental() ? " AUTO" : "")
                        + (c.esNullable() ? "" : " NOT NULL"));
            }

            List<Registro> registros = consultas.leerRegistros(tabla);

            for (Registro r : consultas.leerRegistros(tabla)) {
                System.out.println(r.obtenerValores());
            }
            SqliteService sqlite = new SqliteService("datos_locales.db");
            sqlite.conectar();
            sqlite.guardarTabla(tabla, registros);
            sqlite.cerrar();
            System.out.println("Guardado en SQLite: " + registros.size() + " registros");


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { bd.cerrar(); } catch (SQLException ignored) {}
            tunel.cerrar();
        }

    }
}
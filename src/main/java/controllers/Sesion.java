package controllers;

import database.ConexionRemota;
import database.ConsultasRemotas;
import database.SqliteService;
import database.SshTunnel;
import model.Tabla;

public class Sesion {
    public SshTunnel tunel;
    public ConexionRemota remota;
    public ConsultasRemotas consultas;
    public SqliteService sqlite;
    public Tabla tablaActual;

    public void cerrar() {
        try { if (sqlite != null) sqlite.cerrar(); } catch (Exception ignored) {}
        try { if (remota != null) remota.cerrar(); } catch (Exception ignored) {}
        if (tunel != null) tunel.cerrar();
    }
}

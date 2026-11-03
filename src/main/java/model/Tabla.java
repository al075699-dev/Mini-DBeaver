package model;

import java.util.List;

public class Tabla {


    private String nombre;
    private List<Columnas> columnas;

    public Tabla(
            String name,
            List<Columnas> columnas
    ) {
        this.nombre = name;
        this.columnas = columnas;
    }

    public String obtenerNombre() {
        return nombre;
    }

    public List<Columnas> obtenerColumnas() {
        return columnas;
    }

}

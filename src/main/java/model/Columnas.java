package model;

public class Columnas {
    private String nombre;
    private String tipo;
    private int tamaño;
    private boolean primaryKey;
    private boolean nullable;
    private boolean autoIncremental;

    public Columnas(
            String nombre,
            String tipo,
            int tamaño,
            boolean primaryKey,
            boolean nullable,
            boolean autoIncremental) {

        this.nombre = nombre;
        this.tipo = tipo;
        this.tamaño = tamaño;
        this.primaryKey = primaryKey;
        this.nullable = nullable;
        this.autoIncremental = autoIncremental;
    }

    public String obtenerNombre() {
        return nombre;
    }

    public String obtenerTipo() {
        return tipo;
    }

    public int obtenerTamaño() {
        return tamaño;
    }

    public boolean esPrimaryKey() {
        return primaryKey;
    }

    public boolean esNullable() {
        return nullable;
    }

    public boolean esAutoIncremental() {
        return autoIncremental;
    }
}

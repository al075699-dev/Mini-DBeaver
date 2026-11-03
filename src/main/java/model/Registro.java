package model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Registro {

    //Uso map para registrar la clave de la base de dato con el valor a insertar
    private Map<String, Object> valores;

    public Registro() {
        this.valores = new LinkedHashMap<>();
    }

    public void agregarValor(String columna, Object valor) {
        valores.put(columna, valor);
    }

    public Object obtenerValor(String columna) {
        return valores.get(columna);
    }


    public Map<String, Object> obtenerValores() {
        return valores;
    }


    public void eliminarValor(String columna) {
        valores.remove(columna);
    }
}
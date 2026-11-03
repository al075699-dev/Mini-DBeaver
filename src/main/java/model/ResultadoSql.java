package model;

import java.util.List;

public record ResultadoSql(List<String> columnas, List<Registro> filas,
                           int filasAfectadas, boolean esConsulta) {}
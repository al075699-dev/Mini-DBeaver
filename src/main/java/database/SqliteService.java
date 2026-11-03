package database;

import model.Columnas;
import model.Registro;
import model.ResultadoSql;
import model.Tabla;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SqliteService {

    private final String rutaArchivo;
    private Connection con;

    public SqliteService(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public void conectar() throws SQLException {
        con = DriverManager.getConnection("jdbc:sqlite:" + rutaArchivo);
    }

    public void cerrar() throws SQLException {
        if (con != null && !con.isClosed()) con.close();
    }

    // Crea la tabla local (reemplazando la anterior) e inserta los registros
    public void guardarTabla(Tabla tabla, List<Registro> registros) throws SQLException {
        con.setAutoCommit(false);
        try {
            try (Statement st = con.createStatement()) {
                st.execute("DROP TABLE IF EXISTS " + q(tabla.obtenerNombre()));
                st.execute(armarCreateTable(tabla));
            }
            insertarTodos(tabla, registros);
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private String armarCreateTable(Tabla tabla) {
        List<Columnas> cols = tabla.obtenerColumnas();
        List<String> pks = new ArrayList<>();
        for (Columnas c : cols) {
            if (c.esPrimaryKey()) pks.add(c.obtenerNombre());
        }

        boolean pkSimple = pks.size() == 1;
        StringBuilder sb = new StringBuilder("CREATE TABLE " + q(tabla.obtenerNombre()) + " (");
        List<String> defs = new ArrayList<>();

        for (Columnas c : cols) {
            StringBuilder d = new StringBuilder(q(c.obtenerNombre()) + " " + tipoSqlite(c.obtenerTipo()));
            if (pkSimple && c.esPrimaryKey()) {
                d.append(" PRIMARY KEY");
                if (c.esAutoIncremental()) d.append(" AUTOINCREMENT");
            } else if (!c.esNullable()) {
                d.append(" NOT NULL");
            }
            defs.add(d.toString());
        }


        if (pks.size() > 1) {
            List<String> nombres = new ArrayList<>();
            for (String p : pks) nombres.add(q(p));
            defs.add("PRIMARY KEY (" + String.join(", ", nombres) + ")");
        }

        sb.append(String.join(", ", defs)).append(")");
        return sb.toString();
    }

    private void insertarTodos(Tabla tabla, List<Registro> registros) throws SQLException {
        List<Columnas> cols = tabla.obtenerColumnas();
        List<String> nombres = new ArrayList<>();
        List<String> marcas = new ArrayList<>();
        for (Columnas c : cols) {
            nombres.add(q(c.obtenerNombre()));
            marcas.add("?");
        }
        String sql = "INSERT INTO " + q(tabla.obtenerNombre())
                + " (" + String.join(", ", nombres) + ") VALUES (" + String.join(", ", marcas) + ")";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Registro r : registros) {
                for (int i = 0; i < cols.size(); i++) {
                    ps.setObject(i + 1, convertir(r.obtenerValor(cols.get(i).obtenerNombre())));
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // Traduccion aparentemente necesaria entre sqlite y mariadb
    private String tipoSqlite(String tipoMaria) {
        String t = tipoMaria.toUpperCase().replace("UNSIGNED", "").trim();
        switch (t) {
            case "TINYINT": case "SMALLINT": case "MEDIUMINT":
            case "INT": case "INTEGER": case "BIGINT":
            case "BIT": case "BOOLEAN": case "YEAR":
                return "INTEGER";
            case "FLOAT": case "DOUBLE": case "REAL":
                return "REAL";
            case "DECIMAL": case "NUMERIC":
                return "NUMERIC";
            case "BLOB": case "TINYBLOB": case "MEDIUMBLOB": case "LONGBLOB":
            case "BINARY": case "VARBINARY":
                return "BLOB";
            default:
                return "TEXT";
        }
    }

    // traduccion de valores que sqlite no entiende
    private Object convertir(Object v) {
        if (v == null) return null;
        if (v instanceof BigInteger || v instanceof BigDecimal) return v.toString();
        if (v instanceof java.util.Date) return v.toString();        // Date, Time, Timestamp
        if (v instanceof java.time.temporal.TemporalAccessor) return v.toString();
        return v;
    }

    // Comillas dobles para nombres de tabla/columna
    private String q(String identificador) {
        return "\"" + identificador.replace("\"", "\"\"") + "\"";
    }

    // Leer lo que hay en sqlite
    public List<Registro> leerRegistros(Tabla tabla) throws SQLException {
        List<Registro> lista = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + q(tabla.obtenerNombre()))) {
            while (rs.next()) {
                Registro r = new Registro();
                for (Columnas c : tabla.obtenerColumnas()) {
                    r.agregarValor(c.obtenerNombre(), rs.getObject(c.obtenerNombre()));
                }
                lista.add(r);
            }
        }
        return lista;
    }

    public void insertar(Tabla tabla, Registro nuevo) throws SQLException {
        List<String> nombres = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Columnas c : tabla.obtenerColumnas()) {
            Object v = nuevo.obtenerValor(c.obtenerNombre());
            if (v == null && c.esAutoIncremental()) continue;   // que SQLite asigne el id
            nombres.add(q(c.obtenerNombre()));
            params.add(v);
        }
        String sql;
        if (nombres.isEmpty()) {
            sql = "INSERT INTO " + q(tabla.obtenerNombre()) + " DEFAULT VALUES";
        } else {
            sql = "INSERT INTO " + q(tabla.obtenerNombre()) + " (" + String.join(", ", nombres)
                    + ") VALUES (" + String.join(", ", Collections.nCopies(nombres.size(), "?")) + ")";
        }
        ejecutar(sql, params);
    }

    public void actualizar(Tabla tabla, Registro original, Registro nuevo) throws SQLException {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Columnas c : tabla.obtenerColumnas()) {
            sets.add(q(c.obtenerNombre()) + " = ?");
            params.add(nuevo.obtenerValor(c.obtenerNombre()));
        }
        String sql = "UPDATE " + q(tabla.obtenerNombre()) + " SET " + String.join(", ", sets)
                + armarWhere(tabla, original, params);
        ejecutar(sql, params);
    }

    public void eliminar(Tabla tabla, Registro original) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "DELETE FROM " + q(tabla.obtenerNombre()) + armarWhere(tabla, original, params);
        ejecutar(sql, params);
    }


    private String armarWhere(Tabla tabla, Registro original, List<Object> params) {
        List<Columnas> claves = new ArrayList<>();
        for (Columnas c : tabla.obtenerColumnas()) {
            if (c.esPrimaryKey()) claves.add(c);
        }
        boolean sinPk = claves.isEmpty();
        if (sinPk) claves = tabla.obtenerColumnas();

        List<String> condiciones = new ArrayList<>();
        for (Columnas c : claves) {
            Object v = original.obtenerValor(c.obtenerNombre());
            if (v == null) {
                condiciones.add(q(c.obtenerNombre()) + " IS NULL");
            } else {
                condiciones.add(q(c.obtenerNombre()) + " = ?");
                params.add(v);
            }
        }
        String where = String.join(" AND ", condiciones);

        if (sinPk) {

            return " WHERE rowid = (SELECT rowid FROM " + q(tabla.obtenerNombre())
                    + " WHERE " + where + " LIMIT 1)";
        }
        return " WHERE " + where;
    }

    private void ejecutar(String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, convertir(params.get(i)));
            }
            ps.executeUpdate();
        }
    }

    public boolean existeTabla(String nombre) throws SQLException {
        String sql = "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Tablas que ya están descargadas en SQLite
    public List<String> listarTablasLocales() throws SQLException {
        List<String> nombres = new ArrayList<>();
        String sql = "SELECT name FROM sqlite_master WHERE type = 'table' "
                + "AND name NOT LIKE 'sqlite\\_%' ESCAPE '\\' ORDER BY name";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) nombres.add(rs.getString(1));
        }
        return nombres;
    }

    // Estructura de una tabla leída desde SQLite (sin necesitar el servidor)
    public Tabla obtenerTablaLocal(String nombre) throws SQLException {
        List<String[]> info = new ArrayList<>();   // nombre, tipo, notnull, pk
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + q(nombre) + ")")) {
            while (rs.next()) {
                info.add(new String[] {
                        rs.getString("name"), rs.getString("type"),
                        String.valueOf(rs.getInt("notnull")), String.valueOf(rs.getInt("pk"))
                });
            }
        }
        long totalPk = info.stream().filter(i -> !i[3].equals("0")).count();

        List<Columnas> columnas = new ArrayList<>();
        for (String[] i : info) {
            boolean pk = !i[3].equals("0");
            boolean auto = pk && totalPk == 1 && i[1].equalsIgnoreCase("INTEGER");
            boolean nullable = i[2].equals("0") && !pk;
            columnas.add(new Columnas(i[0], i[1], 0, pk, nullable, auto));
        }
        return new Tabla(nombre, columnas);
    }

    // Ejecuta cualquier sentencia SQL escrita por el usuario
    public ResultadoSql ejecutarSql(String sql) throws SQLException {
        try (Statement st = con.createStatement()) {
            if (st.execute(sql)) {
                try (ResultSet rs = st.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    List<String> columnas = new ArrayList<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String base = meta.getColumnLabel(i);
                        String unico = base;
                        int n = 2;
                        while (columnas.contains(unico)) unico = base + "_" + n++;   // columnas repetidas en JOIN
                        columnas.add(unico);
                    }
                    List<Registro> filas = new ArrayList<>();
                    while (rs.next()) {
                        Registro r = new Registro();
                        for (int i = 1; i <= columnas.size(); i++) {
                            r.agregarValor(columnas.get(i - 1), rs.getObject(i));
                        }
                        filas.add(r);
                    }
                    return new ResultadoSql(columnas, filas, 0, true);
                }
            }
            return new ResultadoSql(List.of(), List.of(), st.getUpdateCount(), false);
        }
    }
}
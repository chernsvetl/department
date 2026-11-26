package ru.nsu.fit.chernyavtseva.department.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;


/**
 * Выгрузка актуального наполнения БД в новую миграцию db/migration/V(n)__data.sql:
 * сначала DELETE всех таблиц, затем INSERT текущих строк и setval счётчиков.
 * Такая миграция при следующем запуске приложения обновит данные в базе
 * (а на новой базе просто создаст то же наполнение).
 *
 * <p>Запуск: java ru.nsu.fit.chernyavtseva.department.db.DataExporter.
 */
public final class DataExporter {

    private static final List<String> TABLES = List.of("department", "person", "thesis", "student", "practice");
    private static final List<String> DELETE_ORDER = List.of("practice", "student", "thesis", "person", "department");
    private static final Path EXPORT_FILE = Path.of("db/data.sql");

    private DataExporter() {
    }

    public static void main(String[] args) throws IOException, SQLException {
        try (Database database = Database.open()) {
            exportToFile(database);
        }
    }

    /**
     * Выгружает актуальное наполнение БД в db/data.sql.
     * Файл генерируется автоматически при запуске Main и обновляется при каждом запуске.
     */
    public static void exportToFile(Database database) throws IOException {
        Files.createDirectories(EXPORT_FILE.getParent());
        Files.writeString(EXPORT_FILE, export(database), StandardCharsets.UTF_8);
        System.out.println("Актуальные данные выгружены в " + EXPORT_FILE.toAbsolutePath());
    }

    static String export(Database database) {
        StringBuilder sql = new StringBuilder();
        for (String table : DELETE_ORDER) {
            sql.append("DELETE FROM ").append(table).append(";\n");
        }
        sql.append('\n');
        for (String table : TABLES) {
            sql.append(inserts(database, table));
            sql.append("SELECT setval(pg_get_serial_sequence('").append(table).append("', 'id'), (SELECT COALESCE(MAX(id), 1) FROM ")
                    .append(table).append("));\n\n");
        }
        return sql.toString();
    }

    private static String inserts(Database database, String table) {
        try (Statement statement = database.connection().createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM " + table + " ORDER BY id")) {
            ResultSetMetaData columns = rows.getMetaData();
            StringBuilder sql = new StringBuilder();
            while (rows.next()) {
                sql.append("INSERT INTO ").append(table).append(" (");
                for (int column = 1; column <= columns.getColumnCount(); column++) {
                    sql.append(column > 1 ? ", " : "").append(columns.getColumnName(column));
                }
                sql.append(") VALUES (");
                for (int column = 1; column <= columns.getColumnCount(); column++) {
                    sql.append(column > 1 ? ", " : "").append(literal(rows.getObject(column)));
                }
                sql.append(");\n");
            }
            return sql.append('\n').toString();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось выгрузить таблицу " + table, e);
        }
    }

    private static String literal(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
}

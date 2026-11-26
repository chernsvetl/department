package ru.nsu.fit.chernyavtseva.department.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.nsu.fit.chernyavtseva.department.TestPostgres;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Выгрузка БД в data.sql и повторное применение выгрузки к чистой базе.
 */
class DataExporterTest {

    private static String exportedSql;

    @BeforeAll
    static void prepareDatabase() {
        TestPostgres.createDatabase("export_source");
        TestPostgres.migrateOntology();
        try (Database database = Database.open()) {
            exportedSql = DataExporter.export(database);
        }
    }

    @Test
    void exportRefreshesDataOfEveryTable() {
        assertTrue(exportedSql.startsWith("DELETE FROM practice;"));
        for (String table : List.of("department", "person", "thesis", "student", "practice")) {
            assertTrue(exportedSql.contains("INSERT INTO " + table + " ("), table);
        }
        assertEquals(74, matches(exportedSql, "INSERT INTO student \\("));
        assertEquals(72, matches(exportedSql, "INSERT INTO practice \\("));
    }

    @Test
    void exportedSqlFillsFreshDatabase() throws Exception {
        String url = TestPostgres.createDatabase("export_target");
        try (Statement statement = DriverManager.getConnection(url, "postgres", "postgres").createStatement()) {
            statement.execute(Files.readString(Path.of("src/main/resources/db/migration/V1__schema.sql")));
            statement.execute(exportedSql);
        }
        try (Statement statement = DriverManager.getConnection(url, "postgres", "postgres").createStatement()) {
            assertEquals(57, count(statement, "SELECT COUNT(*) FROM student WHERE degree = 'Бакалавриат' AND course = 4"));
            assertEquals(17, count(statement, "SELECT COUNT(*) FROM student WHERE degree = 'Магистратура' AND course = 2"));
            assertEquals(74, count(statement, "SELECT COUNT(*) FROM thesis"));
            assertEquals("Пальчунов Дмитрий Евгеньевич",
                    single(statement, "SELECT head_name FROM department WHERE name = 'общей информатики'"));
        }
    }

    @Test
    void identitySequenceContinuesAfterImport() throws Exception {
        String url = TestPostgres.createDatabase("export_sequence");
        try (Statement statement = DriverManager.getConnection(url, "postgres", "postgres").createStatement()) {
            statement.execute(Files.readString(Path.of("src/main/resources/db/migration/V1__schema.sql")));
            statement.execute(exportedSql);
            long maxId = count(statement, "SELECT COALESCE(MAX(id), 0) FROM person");
            try (ResultSet rows = statement.executeQuery("INSERT INTO person (full_name) VALUES ('Тест') RETURNING id")) {
                rows.next();
                assertEquals(maxId + 1, rows.getLong(1), "после явных id из data.sql счётчик должен продолжиться");
            }
        }
    }

    private static int matches(String sql, String regex) {
        return sql.split(regex, -1).length - 1;
    }

    private static long count(Statement statement, String sql) throws SQLException {
        try (ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private static String single(Statement statement, String sql) throws SQLException {
        try (ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getString(1);
        }
    }
}

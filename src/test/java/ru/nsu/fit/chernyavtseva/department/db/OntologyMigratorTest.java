package ru.nsu.fit.chernyavtseva.department.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.nsu.fit.chernyavtseva.department.TestPostgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Перенос онтологии в БД: состав данных, свойства сущностей и пересоздание наполнения при повторном запуске.
 */
class OntologyMigratorTest {

    @BeforeAll
    static void prepareDatabase() throws SQLException {
        TestPostgres.createDatabase("migration");
        TestPostgres.migrateOntology();
    }

    @Test
    void migratesStudentsOfBothDegrees() throws SQLException {
        try (Database database = Database.open()) {
            assertEquals(74, count(database, "student"));
            assertEquals(57, count(database, "student", "degree = 'Бакалавриат' AND course = 4"));
            assertEquals(17, count(database, "student", "degree = 'Магистратура' AND course = 2"));
        }
    }

    @Test
    void migratesThesesAndPractices() throws SQLException {
        try (Database database = Database.open()) {
            assertEquals(74, count(database, "thesis"));
            assertEquals(72, count(database, "practice"));
        }
    }

    @Test
    void departmentKeepsNameAndHeadFromOntology() throws SQLException {
        try (Database database = Database.open()) {
            try (Statement statement = database.connection().createStatement();
                 ResultSet rows = statement.executeQuery("SELECT name, head_name FROM department")) {
                assertTrue(rows.next());
                assertEquals("общей информатики", rows.getString("name"));
                assertEquals("Пальчунов Дмитрий Евгеньевич", rows.getString("head_name"));
                assertFalse(rows.next());
            }
        }
    }

    @Test
    void migratesSupervisorAttributes() throws SQLException {
        try (Database database = Database.open()) {
            try (Statement statement = database.connection().createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT thesis_position_short, academic_degree, ngsu_position, signature_name "
                                 + "FROM person WHERE full_name = 'Барахнин Владимир Борисович'")) {
                assertTrue(rows.next());
                assertEquals("профессор", rows.getString("thesis_position_short"));
                assertEquals("д.т.н.", rows.getString("academic_degree"));
                assertEquals("профессор", rows.getString("ngsu_position"));
                assertEquals("Барахнин В.Б.", rows.getString("signature_name"));
            }
        }
    }

    @Test
    void rerunReplacesDataWithFreshOntologyContent() throws Exception {
        // добавляем в базу строку, которой нет в онтологии
        try (Database database = Database.open()) {
            try (Statement statement = database.connection().createStatement()) {
                statement.execute("INSERT INTO person (full_name) VALUES ('Лишний Руководитель')");
            }
        }
        // повторный запуск миграции должен пересоздать наполнение с нуля
        TestPostgres.migrateOntology();
        try (Database database = Database.open()) {
            assertEquals(74, count(database, "student"));
            assertEquals(74, count(database, "thesis"));
            assertEquals(0, count(database, "person", "full_name = 'Лишний Руководитель'"));
            try (Statement statement = database.connection().createStatement();
                 ResultSet rows = statement.executeQuery("SELECT id, head_name FROM department")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt("id"), "счётчики id сбрасываются при пересоздании");
                assertEquals("Пальчунов Дмитрий Евгеньевич", rows.getString("head_name"));
            }
        }
    }

    private static int count(Database database, String table, String where) throws SQLException {
        try (Statement statement = database.connection().createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + where)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private static int count(Database database, String table) throws SQLException {
        return count(database, table, "1 = 1");
    }
}

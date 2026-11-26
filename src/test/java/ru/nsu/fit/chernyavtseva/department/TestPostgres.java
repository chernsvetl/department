package ru.nsu.fit.chernyavtseva.department;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import ru.nsu.fit.chernyavtseva.department.db.Database;
import ru.nsu.fit.chernyavtseva.department.db.OntologyMigrator;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Общий для тестов встроенный PostgreSQL: запускается один раз на JVM,
 * под каждый тестовый класс создаётся своя пустая база (schema.sql).
 * Docker не нужен.
 */
public final class TestPostgres {

    private static EmbeddedPostgres postgres;

    private TestPostgres() {
    }

    public static synchronized String createDatabase(String name) {
        try {
            if (postgres == null) {
                postgres = EmbeddedPostgres.builder().setCleanDataDirectory(true).start();
                Runtime.getRuntime().addShutdownHook(new Thread(TestPostgres::close));
            }
            String database = "test_" + name;
            try (Connection admin = postgres.getPostgresDatabase().getConnection();
                 Statement statement = admin.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + database);
                statement.execute("CREATE DATABASE " + database);
            }
            System.setProperty("db.url", postgres.getJdbcUrl("postgres", database));
            System.setProperty("db.user", "postgres");
            System.setProperty("db.password", "postgres");
            return postgres.getJdbcUrl("postgres", database);
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Не удалось подготовить тестовую базу данных", e);
        }
    }

    public static void migrateOntology() {
        try (Database database = Database.open()) {
            OntologyMigrator.migrate(database, Path.of("src/main/resources/main_students_all.owl"));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось перенести онтологию в тестовую базу", e);
        }
    }

    private static synchronized void close() {
        try {
            if (postgres != null) {
                postgres.close();
            }
        } catch (IOException ignored) {
            // при остановке JVM уже не важно
        }
    }
}

package ru.nsu.fit.chernyavtseva.department.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

import java.sql.Connection;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Подключение к базе данных PostgreSQL и применение миграций (Flyway).
 *
 * <p>Параметры берутся из classpath-файла db.properties (db.url, db.user, db.password) и могут быть
 * переопределены переменными окружения DB_URL/DB_USER/DB_PASSWORD или системными свойствами -Ddb.url=...
 *
 * <p>Миграции — файлы из classpath-папки db/migration (V1__schema.sql — таблицы,
 * V2__data.sql — начальное наполнение, далее V3, V4...). Applied-миграции фиксируются
 * в таблице flyway_schema_history и повторно не выполняются. Схема существующей базы
 * без истории считается базлинией версии 2 и не перезаполняется данными.
 */
public final class Database implements AutoCloseable {

    private static final String CONFIG_FILE = "/db.properties";
    private static final String MIGRATIONS_LOCATION = "classpath:db/migration";
    private static final String BASELINE_VERSION = "2";
    private static final List<String> SETTINGS = List.of("db.url", "db.user", "db.password");

    private final String url;
    private final Connection connection;

    private Database(String url, Connection connection) {
        this.url = url;
        this.connection = connection;
    }

    public static Database open() {
        Properties config = config();
        String url = config.getProperty("db.url");
        try {
            Flyway.configure()
                    .dataSource(url, config.getProperty("db.user"), config.getProperty("db.password"))
                    .locations(MIGRATIONS_LOCATION)
                    .baselineOnMigrate(true)
                    .baselineVersion(BASELINE_VERSION)
                    .load()
                    .migrate();
            return new Database(url, DriverManager.getConnection(url, config.getProperty("db.user"), config.getProperty("db.password")));
        } catch (FlywayException | SQLException e) {
            throw new IllegalStateException("Не удалось открыть базу данных " + url + ": " + e.getMessage(), e);
        }
    }

    public String url() {
        return url;
    }

    public Connection connection() {
        return connection;
    }

    private static Properties config() {
        Properties config = new Properties();
        try (var in = Database.class.getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                config.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String setting : SETTINGS) {
            String env = System.getenv(setting.toUpperCase(Locale.ROOT).replace('.', '_'));
            if (env != null) {
                config.setProperty(setting, env);
            }
            if (System.getProperty(setting) != null) {
                config.setProperty(setting, System.getProperty(setting));
            }
        }
        return config;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось закрыть соединение с базой данных", e);
        }
    }
}

package ru.nsu.fit.chernyavtseva.department.db;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Подключение к базе данных PostgreSQL и применение миграций (Liquibase).
 *
 * <p>Параметры берутся из classpath-файла db.properties (db.url, db.user, db.password) и могут быть
 * переопределены переменными окружения DB_URL/DB_USER/DB_PASSWORD или системными свойствами -Ddb.url=...
 *
 * <p>Если базы из db.url ещё нет на сервере — она создаётся автоматически.
 *
 * <p>Структуру таблиц создаёт changelog Liquibase из classpath-папки db/changelog
 * (история — в таблицах DATABASECHANGELOG / DATABASECHANGELOGLOCK).
 */
public final class Database implements AutoCloseable {

    private static final String CONFIG_FILE = "/db.properties";
    private static final String CHANGELOG = "db/changelog/changelog-master.xml";
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
            ensureDatabaseExists(url, config);
            Connection connection = DriverManager.getConnection(url, config.getProperty("db.user"), config.getProperty("db.password"));
            Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            liquibase.update();
            return new Database(url, connection);
        } catch (SQLException | LiquibaseException e) {
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
        if (config.getProperty("db.url") == null) {
            throw new IllegalStateException("Не задан db.url — файл db.properties не найден в classpath.\n"
                    + "Запускай приложение через конфигурацию Gradle в IDEA (после импорта build.gradle), "
                    + "либо добавь в VM options: -Ddb.url=jdbc:postgresql://localhost:5432/department "
                    + "-Ddb.user=postgres -Ddb.password=postgres");
        }
        return config;
    }

    /**
     * Если базы из db.url ещё нет на сервере — создаёт её. Liquibase умеет накатывать
     * changelog только на существующую базу, поэтому пустую создаём сами.
     */
    private static void ensureDatabaseExists(String url, Properties config) {
        String database = databaseName(url);
        String serverUrl = url.substring(0, url.lastIndexOf('/')) + "/postgres";
        String user = config.getProperty("db.user");
        String password = config.getProperty("db.password");
        try (Connection connection = DriverManager.getConnection(serverUrl, user, password);
             ResultSet rows = connection.createStatement().executeQuery(
                     "SELECT 1 FROM pg_database WHERE datname = '" + database + "'")) {
            if (!rows.next()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE DATABASE \"" + database + "\"");
                    System.out.println("База данных '" + database + "' не найдена — создана новая");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Сервер PostgreSQL недоступен (" + serverUrl + ").\n"
                    + "Запусти его: docker compose up -d, либо проверь db.url/db.user/db.password.\n"
                    + e.getMessage(), e);
        }
    }

    private static String databaseName(String url) {
        String rest = url.substring(url.indexOf("//") + 2);
        String path = rest.substring(rest.indexOf('/') + 1);
        int query = path.indexOf('?');
        return query < 0 ? path : path.substring(0, query);
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

package ru.nsu.fit.chernyavtseva.department.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.nsu.fit.chernyavtseva.department.TestPostgres;

import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void migratesFormSupervisorProperty(@TempDir Path dir) throws Exception {
        // веб-форма отправляет руководителя практики от НГУ свойством пишет_вкр_у:
        // у Иванова — только пишет_вкр_у, у Петрова — и на_НГУ_практике_у (приоритетнее)
        Path owl = dir.resolve("form.owl");
        Files.writeString(owl, ONTOLOGY_WITH_FORM_PROPERTIES);
        try (Database database = Database.open()) {
            OntologyMigrator.migrate(database, owl);
            assertEquals("Петров Пётр Петрович", practiceNgsuSupervisor(database, "Иванов Иван"));
            assertEquals("Сидоров Сидор Сидорович", practiceNgsuSupervisor(database, "Пётр Петров"));
        }
        // возвращаем базе наполнение из основной онтологии — от него зависят остальные тесты
        TestPostgres.migrateOntology();
    }

    @Test
    void fallsBackToFileWhenFusekiUnavailable() throws Exception {
        String fuseki = System.getProperty("ontology.fuseki");
        System.setProperty("ontology.fuseki", "http://localhost:1/ontology/data");
        try (Database database = Database.open()) {
            // Fuseki недоступен — данные берутся из файла онтологии (ontology.file),
            // при его отсутствии — из classpath-ресурса main_students_all.owl
            assertTrue(OntologyMigrator.migrate(database).contains("main_students_all.owl"));
        } finally {
            if (fuseki == null) {
                System.clearProperty("ontology.fuseki");
            } else {
                System.setProperty("ontology.fuseki", fuseki);
            }
        }
    }

    private static String practiceNgsuSupervisor(Database database, String student) throws SQLException {
        try (Statement statement = database.connection().createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT person.full_name FROM practice "
                             + "JOIN student ON student.id = practice.student_id "
                             + "JOIN person ON person.id = practice.ngsu_supervisor_id "
                             + "WHERE student.full_name = '" + student + "'")) {
            assertTrue(rows.next(), "руководитель практики не найден: " + student);
            String name = rows.getString(1);
            assertFalse(rows.next());
            return name;
        }
    }

    private static final String ONTOLOGY_WITH_FORM_PROPERTIES = """
            <?xml version="1.0"?>
            <rdf:RDF xmlns="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#"
                 xml:base="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра"
                 xmlns:owl="http://www.w3.org/2002/07/owl#"
                 xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <owl:NamedIndividual rdf:about="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#ИвановИван">
                    <rdf:type rdf:resource="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#Бакалавриат"/>
                    <ФИО>Иванов Иван</ФИО>
                    <группа>ПИиКН-4-01</группа>
                    <пишет_вкр_у rdf:resource="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#ПетровПетр"/>
                </owl:NamedIndividual>
                <owl:NamedIndividual rdf:about="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#ПётрПетров">
                    <rdf:type rdf:resource="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#Бакалавриат"/>
                    <ФИО>Пётр Петров</ФИО>
                    <группа>ПИиКН-4-01</группа>
                    <пишет_вкр_у rdf:resource="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#СидоровСидор"/>
                    <на_НГУ_практике_у rdf:resource="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#СидоровСидор"/>
                </owl:NamedIndividual>
                <owl:NamedIndividual rdf:about="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#ПетровПетр">
                    <rdf:type rdf:resource="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#Руководитель_от_НГУ"/>
                    <ФИО>Петров Пётр Петрович</ФИО>
                </owl:NamedIndividual>
                <owl:NamedIndividual rdf:about="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#СидоровСидор">
                    <rdf:type rdf:resource="http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#Руководитель_от_НГУ"/>
                    <ФИО>Сидоров Сидор Сидорович</ФИО>
                </owl:NamedIndividual>
            </rdf:RDF>
            """;

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

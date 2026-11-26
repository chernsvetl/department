package ru.nsu.fit.chernyavtseva.department.query_model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.nsu.fit.chernyavtseva.department.Degree;
import ru.nsu.fit.chernyavtseva.department.TestPostgres;
import ru.nsu.fit.chernyavtseva.department.db.Database;

import java.sql.SQLException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * SQL-запрос студентов из БД (замена прежнего SPARQL-запроса к онтологии).
 */
class QueryExecutorTest {

    @BeforeAll
    static void prepareDatabase() {
        TestPostgres.createDatabase("query");
        TestPostgres.migrateOntology();
    }

    @Test
    void selectsStudentsOfEachDegree() {
        try (Database database = Database.open()) {
            QueryExecutor executor = new QueryExecutor(database);
            assertEquals(51, executor.findStudents(Degree.all()[0]).size(), "бакалавриат 4 курс ПИиКН");
            assertEquals(5, executor.findStudents(Degree.all()[1]).size(), "бакалавриат 4 курс КНиС");
            assertEquals(3, executor.findStudents(Degree.all()[2]).size(), "магистратура 2 курс МДА");
            assertEquals(13, executor.findStudents(Degree.all()[3]).size(), "магистратура 2 курс ТРПС");
        }
    }

    @Test
    void rowsCarryVariablesForDocumentKeywords() {
        try (Database database = Database.open()) {
            List<Map<String, String>> students = new QueryExecutor(database).findStudents(Degree.all()[0]);
            Map<String, String> student = students.get(0);

            assertEquals("Авдеев Данил Сергеевич", student.get("фио_студента"));
            assertEquals("20208", student.get("группа_студента"));
            assertNotNull(student.get("тема_вкр"));
            assertEquals("общей информатики", student.get("название_кафедры"));
            assertEquals("Пальчунов Дмитрий Евгеньевич", student.get("заведующий_кафедры"));
            assertEquals("Барахнин Владимир Борисович", student.get("фио_руководителя"));
            assertEquals("профессор", student.get("должность_руководителя_вкр_кратко"));
            assertEquals("д.т.н.", student.get("ученая_степень_руководителя_ВКР"));
            assertNotNull(student.get("фио_НГУ_руководителя"));
            assertNotNull(student.get("фио_орг_руководителя"));
            assertNotNull(student.get("место_практики_полное_наименование"));
            assertNotNull(student.get("приказ_практика"));
        }
    }
}

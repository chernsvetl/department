package ru.nsu.fit.chernyavtseva.department.query_model;

import ru.nsu.fit.chernyavtseva.department.Degree;
import ru.nsu.fit.chernyavtseva.department.db.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Выполняет SQL-запрос студентов для выбранной группы ({@link Degree}) и возвращает строки
 * в виде "имя переменной шаблона -> значение" (фио_студента, тема_вкр и т.д.).
 */
public class QueryExecutor {

    private static final Map<String, String> FIELD_BY_COLUMN = Map.ofEntries(
            Map.entry("student_full_name", "фио_студента"),
            Map.entry("student_group", "группа_студента"),
            Map.entry("department_name", "название_кафедры"),
            Map.entry("department_head", "заведующий_кафедры"),
            Map.entry("thesis_topic", "тема_вкр"),
            Map.entry("supervisor_full_name", "фио_руководителя"),
            Map.entry("supervisor_position", "должность_руководителя_вкр"),
            Map.entry("supervisor_position_short", "должность_руководителя_вкр_кратко"),
            Map.entry("supervisor_degree", "ученая_степень_руководителя_ВКР"),
            Map.entry("co_supervisor_full_name", "фио_соруководителя_вкр"),
            Map.entry("co_supervisor_position", "должность_соруководителя_вкр"),
            Map.entry("reviewer_full_name", "фио_рецензента"),
            Map.entry("reviewer_position", "должность_рецензента"),
            Map.entry("practice_place", "место_практики"),
            Map.entry("practice_full_place", "место_практики_полное_наименование"),
            Map.entry("practice_organization", "наименование_организации"),
            Map.entry("practice_order", "приказ_практика"),
            Map.entry("ngsu_supervisor_full_name", "фио_НГУ_руководителя"),
            Map.entry("ngsu_supervisor_position", "должность_НГУ_руководителя"),
            Map.entry("organization_supervisor_full_name", "фио_орг_руководителя"),
            Map.entry("organization_supervisor_position", "должность_орг_руководителя"),
            Map.entry("organization_supervisor_signature", "фио_подпись"),
            Map.entry("organization_supervisor_bachelor_date", "бак_дата_рук"),
            Map.entry("organization_supervisor_bachelor_knis_date", "бак_книс_дата_рук"),
            Map.entry("organization_supervisor_master_mda_date", "маг_дата_рук_мда"),
            Map.entry("organization_supervisor_master_trps_date", "маг_дата_рук_трпс")
    );

    private final Database database;

    public QueryExecutor(Database database) {
        this.database = database;
    }

    public List<Map<String, String>> findStudents(Degree degree) {
        StudentsQuery query = new StudentsQuery(degree);
        List<Map<String, String>> students = new ArrayList<>();
        try (PreparedStatement statement = database.connection().prepareStatement(query.create())) {
            statement.setString(1, query.degree());
            statement.setInt(2, query.course());
            statement.setString(3, query.profile());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    students.add(student(rows));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось получить студентов из базы данных", e);
        }
        return students;
    }

    private static Map<String, String> student(ResultSet rows) throws SQLException {
        Map<String, String> student = new HashMap<>();
        for (Map.Entry<String, String> field : FIELD_BY_COLUMN.entrySet()) {
            student.put(field.getValue(), rows.getString(field.getKey()));
        }
        return student;
    }
}

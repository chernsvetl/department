package ru.nsu.fit.chernyavtseva.department.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Записывает данные о кафедре ({@link DepartmentData}) в таблицы БД:
 * department, person, thesis, student, practice.
 *
 * <p>Все изменения выполняются в одной транзакции: {@link #commit()} фиксирует её,
 * {@link #close()} без commit откатывает. Повторная запись тех же людей/тем/студентов
 * обновляет существующие строки (upsert), пустые поля существующих строк не затираются.
 */
public final class DbWriter implements AutoCloseable {

    private static final String PERSON_SQL = personSql();

    private final Connection connection;
    private final boolean autoCommit;
    private boolean committed;

    public DbWriter(Database database) {
        connection = database.connection();
        try {
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось начать транзакцию", e);
        }
    }

    public void saveAll(DepartmentData data) {
        try {
            int departmentId = saveDepartment(data.departmentName(), data.headName());
            Map<PersonData, Integer> personIds = new HashMap<>();
            for (PersonData person : data.persons()) {
                personIds.put(person, savePerson(person));
            }
            Set<ThesisData> theses = new HashSet<>();
            for (StudentData student : data.students()) {
                if (student.thesis() != null) {
                    theses.add(student.thesis());
                }
            }
            Map<ThesisData, Integer> thesisIds = new HashMap<>();
            for (ThesisData thesis : theses) {
                thesisIds.put(thesis, saveThesis(thesis, personIds));
            }
            for (StudentData student : data.students()) {
                Integer thesisId = student.thesis() == null ? null : thesisIds.get(student.thesis());
                int studentId = saveStudent(student, thesisId, departmentId);
                if (!student.practice().isEmpty()) {
                    savePractice(student.practice(), studentId, personIds);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить данные в базе данных: " + e.getMessage(), e);
        }
    }

    public void commit() {
        try {
            connection.commit();
            committed = true;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось зафиксировать изменения в базе данных", e);
        }
    }

    private void update(String sql, String error, Object... parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(error, e);
        }
    }

    private int saveDepartment(String name, String headName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO department (name, head_name) VALUES (?, ?) "
                        + "ON CONFLICT (name) DO UPDATE SET head_name = COALESCE(EXCLUDED.head_name, department.head_name) "
                        + "RETURNING id")) {
            statement.setString(1, name);
            statement.setString(2, Text.isBlank(headName) ? null : headName);
            return insert(statement);
        }
    }

    private int savePerson(PersonData person) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(PERSON_SQL)) {
            statement.setString(1, person.fullName());
            int index = 2;
            for (PersonField field : PersonField.values()) {
                statement.setString(index++, person.get(field));
            }
            return insert(statement);
        }
    }

    private int saveThesis(ThesisData thesis, Map<PersonData, Integer> personIds) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO thesis (topic, supervisor_id, co_supervisor_id, consultant_id, reviewer_id) VALUES (?, ?, ?, ?, ?) "
                        + "ON CONFLICT (topic) DO UPDATE SET "
                        + "supervisor_id = COALESCE(EXCLUDED.supervisor_id, thesis.supervisor_id), "
                        + "co_supervisor_id = COALESCE(EXCLUDED.co_supervisor_id, thesis.co_supervisor_id), "
                        + "consultant_id = COALESCE(EXCLUDED.consultant_id, thesis.consultant_id), "
                        + "reviewer_id = COALESCE(EXCLUDED.reviewer_id, thesis.reviewer_id) "
                        + "RETURNING id")) {
            statement.setString(1, thesis.topic());
            setId(statement, 2, id(thesis.supervisor(), personIds));
            setId(statement, 3, id(thesis.coSupervisor(), personIds));
            setId(statement, 4, id(thesis.consultant(), personIds));
            setId(statement, 5, id(thesis.reviewer(), personIds));
            return insert(statement);
        }
    }

    private int saveStudent(StudentData student, Integer thesisId, int departmentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO student (department_id, degree, course, full_name, group_name, profile, thesis_id) VALUES (?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT (degree, course, full_name, group_name) DO UPDATE SET "
                        + "department_id = EXCLUDED.department_id, "
                        + "profile = COALESCE(EXCLUDED.profile, student.profile), "
                        + "thesis_id = COALESCE(EXCLUDED.thesis_id, student.thesis_id) "
                        + "RETURNING id")) {
            statement.setInt(1, departmentId);
            statement.setString(2, student.degree());
            statement.setInt(3, student.course());
            statement.setString(4, student.fullName());
            statement.setString(5, student.group());
            statement.setString(6, Text.isBlank(student.profile()) ? null : student.profile());
            setId(statement, 7, thesisId);
            return insert(statement);
        }
    }

    private void savePractice(PracticeData practice, int studentId, Map<PersonData, Integer> personIds) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO practice (student_id, kind, place, full_place, organization_name, order_number, "
                        + "ngsu_supervisor_id, organization_supervisor_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT (student_id) DO UPDATE SET "
                        + "kind = COALESCE(EXCLUDED.kind, practice.kind), "
                        + "place = COALESCE(EXCLUDED.place, practice.place), "
                        + "full_place = COALESCE(EXCLUDED.full_place, practice.full_place), "
                        + "organization_name = COALESCE(EXCLUDED.organization_name, practice.organization_name), "
                        + "order_number = COALESCE(EXCLUDED.order_number, practice.order_number), "
                        + "ngsu_supervisor_id = COALESCE(EXCLUDED.ngsu_supervisor_id, practice.ngsu_supervisor_id), "
                        + "organization_supervisor_id = COALESCE(EXCLUDED.organization_supervisor_id, practice.organization_supervisor_id)")) {
            statement.setInt(1, studentId);
            statement.setString(2, value(practice.kind()));
            statement.setString(3, value(practice.place()));
            statement.setString(4, value(practice.fullPlace()));
            statement.setString(5, value(practice.organizationName()));
            statement.setString(6, value(practice.order()));
            setId(statement, 7, id(practice.ngsuSupervisor(), personIds));
            setId(statement, 8, id(practice.organizationSupervisor(), personIds));
            statement.executeUpdate();
        }
    }

    private static int insert(PreparedStatement statement) throws SQLException {
        try (ResultSet row = statement.executeQuery()) {
            row.next();
            return row.getInt(1);
        }
    }

    private static void setId(PreparedStatement statement, int index, Integer id) throws SQLException {
        if (id == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, id);
        }
    }

    private static Integer id(PersonData person, Map<PersonData, Integer> personIds) {
        return person == null ? null : personIds.get(person);
    }

    private static String value(String text) {
        return Text.isBlank(text) ? null : text;
    }

    private static String personSql() {
        String columns = Stream.of(PersonField.values()).map(PersonField::column).collect(Collectors.joining(", "));
        String placeholders = String.join(", ", Collections.nCopies(PersonField.values().length + 1, "?"));
        String updates = Stream.of(PersonField.values())
                .map(field -> field.column() + " = COALESCE(EXCLUDED." + field.column() + ", person." + field.column() + ")")
                .collect(Collectors.joining(", "));
        return "INSERT INTO person (full_name, " + columns + ") VALUES (" + placeholders + ") "
                + "ON CONFLICT (full_name) DO UPDATE SET " + updates + " RETURNING id";
    }

    @Override
    public void close() {
        try {
            if (!committed) {
                connection.rollback();
            }
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось завершить транзакцию", e);
        }
    }
}

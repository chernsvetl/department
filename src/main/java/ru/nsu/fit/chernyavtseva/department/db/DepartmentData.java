package ru.nsu.fit.chernyavtseva.department.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Данные одной кафедры, собранные {@link OntologyMigrator} из файла онтологии
 * и подготовленные для записи в БД: руководители и рецензенты, темы ВКР
 * и студенты с их практикой.
 */
public final class DepartmentData {

    private final String departmentName;
    private String headName;
    private final Map<String, PersonData> persons = new LinkedHashMap<>();
    private final Map<String, ThesisData> theses = new LinkedHashMap<>();
    private final List<StudentData> students = new ArrayList<>();

    public DepartmentData(String departmentName, String headName) {
        this.departmentName = Text.clean(departmentName);
        this.headName = Text.clean(headName);
    }

    public String departmentName() {
        return departmentName;
    }

    public String headName() {
        return headName;
    }

    public Collection<PersonData> persons() {
        return persons.values();
    }

    public List<StudentData> students() {
        return students;
    }

    public PersonData person(String fullName) {
        String name = Text.name(fullName);
        if (Text.isBlank(name)) {
            throw new IllegalArgumentException("Пустое ФИО");
        }
        return persons.computeIfAbsent(name, PersonData::new);
    }

    public ThesisData thesis(String topic) {
        String cleaned = Text.clean(topic);
        if (Text.isBlank(cleaned)) {
            throw new IllegalArgumentException("Пустая тема ВКР");
        }
        return theses.computeIfAbsent(cleaned, ThesisData::new);
    }

    public StudentData addStudent(String degree, int course, String fullName, String group, String profile) {
        String name = Text.name(fullName);
        if (Text.isBlank(name)) {
            throw new IllegalArgumentException("Пустое ФИО студента");
        }
        StudentData student = new StudentData(degree, course, name, Text.isBlank(group) ? "" : Text.name(group), profile);
        students.add(student);
        return student;
    }
}

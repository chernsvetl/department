package ru.nsu.fit.chernyavtseva.department.db;

/**
 * Студент (Бакалавриат/Магистратура): ФИО, группа, профиль, тема ВКР ({@link ThesisData})
 * и практика ({@link PracticeData}) — таблицы student и practice.
 */
public final class StudentData {

    private final String degree;
    private final int course;
    private final String fullName;
    private String group;
    private String profile;
    private ThesisData thesis;
    private final PracticeData practice = new PracticeData();

    StudentData(String degree, int course, String fullName, String group, String profile) {
        this.degree = degree;
        this.course = course;
        this.fullName = fullName;
        this.group = group;
        this.profile = profile;
    }

    public String degree() {
        return degree;
    }

    public int course() {
        return course;
    }

    public String fullName() {
        return fullName;
    }

    public String group() {
        return group;
    }

    public String profile() {
        return profile;
    }

    public ThesisData thesis() {
        return thesis;
    }

    public PracticeData practice() {
        return practice;
    }

    public void setThesis(ThesisData thesis) {
        if (thesis != null && this.thesis == null) {
            this.thesis = thesis;
        }
    }
}

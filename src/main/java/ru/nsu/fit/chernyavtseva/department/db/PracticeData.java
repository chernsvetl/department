package ru.nsu.fit.chernyavtseva.department.db;

/**
 * Практика студента: вид практики, место прохождения (короткое и полное), организация,
 * приказ о направлении и руководители практики (от НГУ и от организации) — таблица practice.
 */
public final class PracticeData {

    private String kind;
    private String place;
    private String fullPlace;
    private String organizationName;
    private String order;
    private PersonData ngsuSupervisor;
    private PersonData organizationSupervisor;

    public String kind() {
        return kind;
    }

    public String place() {
        return place;
    }

    public String fullPlace() {
        return fullPlace;
    }

    public String organizationName() {
        return organizationName;
    }

    public String order() {
        return order;
    }

    public PersonData ngsuSupervisor() {
        return ngsuSupervisor;
    }

    public PersonData organizationSupervisor() {
        return organizationSupervisor;
    }

    public void setKind(String kind) {
        this.kind = Text.clean(kind);
    }

    public void setPlace(String place) {
        this.place = Text.clean(place);
    }

    public void setFullPlace(String fullPlace) {
        this.fullPlace = Text.clean(fullPlace);
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = Text.clean(organizationName);
    }

    public void setOrder(String order) {
        this.order = Text.clean(order);
    }

    public void setNgsuSupervisor(PersonData ngsuSupervisor) {
        if (this.ngsuSupervisor == null) {
            this.ngsuSupervisor = ngsuSupervisor;
        }
    }

    public void setOrganizationSupervisor(PersonData organizationSupervisor) {
        if (this.organizationSupervisor == null) {
            this.organizationSupervisor = organizationSupervisor;
        }
    }

    public boolean isEmpty() {
        return ngsuSupervisor == null && organizationSupervisor == null
                && Text.isBlank(kind) && Text.isBlank(place) && Text.isBlank(fullPlace)
                && Text.isBlank(organizationName) && Text.isBlank(order);
    }
}

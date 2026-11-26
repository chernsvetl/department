package ru.nsu.fit.chernyavtseva.department.db;

/**
 * ВКР: тема и связи с руководителем, соруководителем, консультантом и рецензентом — таблица thesis.
 *
 */
public final class ThesisData {

    private final String topic;
    private PersonData supervisor;
    private PersonData coSupervisor;
    private PersonData consultant;
    private PersonData reviewer;

    ThesisData(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }

    public PersonData supervisor() {
        return supervisor;
    }

    public PersonData coSupervisor() {
        return coSupervisor;
    }

    public PersonData consultant() {
        return consultant;
    }

    public PersonData reviewer() {
        return reviewer;
    }

    public void supervisor(PersonData supervisor) {
        if (this.supervisor == null && supervisor != null) {
            this.supervisor = supervisor;
        }
    }

    public void coSupervisor(PersonData coSupervisor) {
        if (this.coSupervisor == null && coSupervisor != null) {
            this.coSupervisor = coSupervisor;
        }
    }

    public void consultant(PersonData consultant) {
        if (this.consultant == null && consultant != null) {
            this.consultant = consultant;
        }
    }

    public void reviewer(PersonData reviewer) {
        if (this.reviewer == null && reviewer != null) {
            this.reviewer = reviewer;
        }
    }

    public void merge(ThesisData other) {
        supervisor(other.supervisor);
        coSupervisor(other.coSupervisor);
        consultant(other.consultant);
        reviewer(other.reviewer);
    }
}

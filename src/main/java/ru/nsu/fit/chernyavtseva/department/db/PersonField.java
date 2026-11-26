package ru.nsu.fit.chernyavtseva.department.db;

/**
 * Свойства человека из онтологии (Должность_руководителя_ВКР, Ученая_степень_руководителя_ВКР,
 * ФИО_для_подписи, ДатаБакРук и т.д.) и одноимённые колонки таблицы person.
 */
public enum PersonField {
    THESIS_POSITION("thesis_position"),
    THESIS_POSITION_SHORT("thesis_position_short"),
    ACADEMIC_DEGREE("academic_degree"),
    POSITION("position"),
    NGSU_POSITION("ngsu_position"),
    ORGANIZATION_POSITION("organization_position"),
    SIGNATURE_NAME("signature_name"),
    BACHELOR_DATE("bachelor_date"),
    BACHELOR_KNIS_DATE("bachelor_knis_date"),
    MASTER_MDA_DATE("master_mda_date"),
    MASTER_TRPS_DATE("master_trps_date");

    private final String column;

    PersonField(String column) {
        this.column = column;
    }

    String column() {
        return column;
    }
}

package ru.nsu.fit.chernyavtseva.department.db;

import java.util.EnumMap;
import java.util.Map;

/**
 * Человек из онтологии (Руководитель_ВКР, Соруководитель_ВКР, Консультант, Руководитель_от_НГУ,
 * Руководитель_от_организации, Рецензент) — строка таблицы person.
 * Один человек может совмещать несколько ролей, поэтому все должности — в одной записи.
 */
public final class PersonData {

    private final String fullName;
    private final Map<PersonField, String> values = new EnumMap<>(PersonField.class);

    PersonData(String fullName) {
        this.fullName = fullName;
    }

    public String fullName() {
        return fullName;
    }

    public String get(PersonField field) {
        return values.get(field);
    }

    public void set(PersonField field, String value) {
        String cleaned = Text.clean(value);
        if (!Text.isBlank(cleaned)) {
            putIfBlank(field, cleaned);
        }
    }

    private void putIfBlank(PersonField field, String value) {
        if (Text.isBlank(values.get(field))) {
            values.put(field, value);
        }
    }
}

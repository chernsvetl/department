package ru.nsu.fit.chernyavtseva.department.db;

/**
 * Нормализация строк из онтологии: неразрывные пробелы, лишние пробелы по краям и внутри.
 */
final class Text {

    private Text() {
    }

    static String clean(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\u00A0', ' ').strip();
    }

    static String name(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.replaceAll("\\s+", " ");
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

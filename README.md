# department — помощник секретаря кафедры

Заполняет документы кафедры по .docx-шаблонам, подставляя данные студентов
(тема ВКР, руководители, практика и т.д.) из базы данных PostgreSQL.
Раньше данные хранились в онтологии (Protege + SPARQL), теперь — в БД.

## Запуск
`./gradlew run` (или класс `ru.nsu.fit.chernyavtseva.department.Main` из IDEA) —
при каждом запуске он накатывает миграции Liquibase (`src/main/resources/db/changelog`:
структура таблиц), пересоздаёт наполнение базы из онтологии
(`src/main/resources/main_students_all.owl`), затем генерирует документы
всех групп студентов (4 курс ПИиКН/КНиС, 2 курс магистратура МДА/ТРПС)
в `build/classes/java/main/documents/...`.

## Сборка, тесты и пайплайн
Проект на Gradle (wrapper включён, локально ничего ставить не нужно):
- `./gradlew test` — тесты (миграция онтологии, SQL-выборки, заполнение шаблонов);
  тесты поднимают встроенный PostgreSQL, Docker не нужен;
- `./gradlew checkstyleMain checkstyleTest` — проверка стиля;
- `./gradlew assemble` — сборка `department-1.0-SNAPSHOT.jar`.

Пайплайн CI: `.github/workflows/ci.yml` — 1) чек стайл (Checkstyle) -> 2) сборка -> 3) тесты.

## Наполнение базы и структура проекта
см. [instruction.md](instruction.md)

- `src/main/resources/db/changelog/` — миграции Liquibase: структура таблиц базы
- `src/main/resources/templates/...` — шаблоны документов с ключевыми словами
- `tables/...` — Excel-таблицы кафедры (источник данных для загрузки в БД)
- `src/main/resources/main_students_all.owl` — файл прежней онтологии (используется OntologyMigrator)

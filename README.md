# department — помощник секретаря кафедры

Заполняет документы кафедры по .docx-шаблонам, подставляя данные студентов
(тема ВКР, руководители, практика и т.д.) из базы данных PostgreSQL.
Раньше данные хранились в онтологии (Protege + SPARQL), теперь — в БД.

## Запуск
2. Запустить класс `ru.nsu.fit.chernyavtseva.department.Main` —
   при первом подключении он накатывает миграции Flyway (`src/main/resources/db/migration`:
   таблицы + начальные данные), затем генерирует документы
   всех групп студентов (4 курс ПИиКН/КНиС, 2 курс магистратура МДА/ТРПС)
   в `target/classes/documents/...`.

## Сборка, тесты и пайплайн
Проект на Gradle (wrapper включён, локально ничего ставить не нужно):
- `./gradlew test` — 16 тестов (миграция онтологии, SQL-выборки, заполнение шаблонов, выгрузка data.sql);
  тесты поднимают встроенный PostgreSQL, Docker не нужен;
- `./gradlew checkstyleMain checkstyleTest` — проверка стиля;
- `./gradlew assemble` — сборка `department-1.0-SNAPSHOT.jar`.

Пайплайн CI: `.github/workflows/ci.yml` — 1) чек стайл (Checkstyle) -> 2) сборка -> 3) тесты.

## Наполнение базы и структура проекта
см. [instruction.md](instruction.md)

- `src/main/resources/db/migration/` — миграции Flyway: таблицы и начальное наполнение базы
- `src/main/resources/templates/...` — шаблоны документов с ключевыми словами
- `tables/...` — Excel-таблицы кафедры (источник данных для загрузки в БД)
- `src/main/resources/main_students_all.owl` — файл прежней онтологии (используется OntologyMigrator)

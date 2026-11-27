# department — помощник секретаря кафедры

Заполняет документы кафедры по .docx-шаблонам, подставляя данные студентов
(тема ВКР, руководители, практика и т.д.) из базы данных PostgreSQL.
Данные студентов приходят из онтологии: веб-форма системы сбора пишет их в граф Fuseki,
откуда они переносятся в БД и в файл онтологии `main_students_all.owl`.

## Запуск
`./gradlew run` (или класс `ru.nsu.fit.chernyavtseva.department.Main` из IDEA) —
при каждом запуске он накатывает миграции Liquibase (`src/main/resources/db/changelog`:
структура таблиц), пересоздаёт наполнение базы из онтологии — сначала из графа Fuseki
(`ontology.fuseki` в `db.properties`, по умолчанию `http://localhost:3030/ontology/data`;
граф при этом сохраняется в файл онтологии `ontology.file`; если Fuseki недоступен —
из файла `src/main/java/ru/nsu/fit/chernyavtseva/department/ontology/main_students_all.owl`),
затем генерирует документы
всех групп студентов (4 курс ПИиКН/КНиС, 2 курс магистратура МДА/ТРПС)
в `build/classes/java/main/documents/...`.

## Система сбора заполнений (src/main/ontology)
Веб-форма студентов + Fuseki + перенос в БД: `src/main/ontology/`
(подробности — в `src/main/ontology/README.md`):
- `cd src/main/ontology && docker compose up -d` — поднимет Fuseki с онтологией кафедры;
- заполнения из веб-формы попадают в граф Fuseki (SPARQL UPDATE);
- сразу после отправки формы бэкенд запускает `./gradlew migrateOntology` —
  граф Fuseki переносится в PostgreSQL и сохраняется в файл `main_students_all.owl`;
- `./gradlew migrateOntology` можно выполнить и вручную в любой момент.

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
- `src/main/java/ru/nsu/fit/chernyavtseva/department/ontology/main_students_all.owl` — файл
  онтологии: синхронная копия графа Fuseki (обновляется при каждом переносе) и фолбэк,
  если Fuseki недоступен
- `src/main/ontology/` — система сбора заполнений (веб-форма, Spring Boot + React + Fuseki)

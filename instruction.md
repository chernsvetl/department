# Как наполнить базу данных кафедры

## 1. Запустить PostgreSQL
`docker compose up -d` — поднимет базу `department` (пользователь postgres / пароль postgres).
Свой сервер можно указать в `src/main/resources/db.properties`
(или переменными окружения `DB_URL`, `DB_USER`, `DB_PASSWORD`).

## 2. Миграции
Таблицы и начальное наполнение создаются миграциями Flyway из `src/main/resources/db/migration`:
- `V1__schema.sql` — таблицы (department, person, thesis, student, practice);
- `V2__data.sql` — начальное наполнение.

Миграции накатываются автоматически при запуске приложения (история — в таблице
`flyway_schema_history`). Можно выполнить их и вручную, например `./gradlew flywayMigrate` (нужен плагин) или через pgAdmin.

## 3. Наполнить базу данными
Из прежней онтологии (`src/main/resources/main_students_all.owl`):
`java -cp build/libs/department-1.0-SNAPSHOT.jar ru.nsu.fit.chernyavtseva.department.db.OntologyMigrator src/main/resources/main_students_all.owl`
Мигратор дозаполняет базу: руководители/рецензенты, темы ВКР, студенты и их практики
обновляются по ключам, существующие строки не дублируются.

## 4. Сохранить актуальное наполнение в новую миграцию
Чтобы данные из базы попадали в свежие базы автоматически:
`java ru.nsu.fit.chernyavtseva.department.db.DataExporter` —
создаст следующую миграцию `V(n)__data.sql` (DELETE + актуальные INSERT).

## Генерация документов
`ru.nsu.fit.chernyavtseva.department.Main` — подключается к базе, накатывает миграции
и генерирует документы всех групп
(`Degree`: бакалавриат 4 курс ПИиКН/КНиС, магистратура 2 курс МДА/ТРПС):
шаблоны из `src/main/resources/templates/<группа>`, результат
в `target/classes/documents/<группа>`, имя файла начинается с ФИО студента.
У некоторых групп список шаблонов закомментирован в `Degree.java` —
раскомментируй нужные строки, чтобы они тоже генерировались.
Если каких-то данных в базе нет, ключевое слово останется в документе,
а в консоль будет выведено предупреждение.

## Легаси: онтология через Protege
Прежний способ наполнения (Protege + Cellfie + SPARQL) заменён базой данных.
Из онтологии остался только файл данных `src/main/resources/main_students_all.owl`
(именно его читает `OntologyMigrator`); TBox-схема теперь живёт в `db/schema.sql`,
правила Cellfie и инструкция по Protege удалены (доступны в истории git).

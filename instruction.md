# Как наполнить базу данных кафедры

## 1. Запустить PostgreSQL
`docker compose up -d` — поднимет базу `department` (пользователь postgres / пароль postgres).
Свой сервер можно указать в `src/main/resources/db.properties`
(или переменными окружения `DB_URL`, `DB_USER`, `DB_PASSWORD`).

## 2. Миграции
Структуру таблиц (department, person, thesis, student, practice) создаёт Liquibase
из `src/main/resources/db/changelog` — миграции накатываются автоматически при запуске
приложения (история — в таблицах `DATABASECHANGELOG` / `DATABASECHANGELOGLOCK`).

## 3. Наполнить базу данными
Из прежней онтологии (`src/main/resources/main_students_all.owl`) — это происходит
автоматически при каждом запуске `Main`: таблицы очищаются и заново наполняются
данными онтологии (`OntologyMigrator`), поэтому наполнение базы всегда совпадает
с онтологией. Отдельный запуск: `java ru.nsu.fit.chernyavtseva.department.db.OntologyMigrator`
(или с путём к другому .owl-файлу) делает то же самое.

## Генерация документов
`ru.nsu.fit.chernyavtseva.department.Main` — подключается к базе, накатывает миграции,
пересоздаёт наполнение из онтологии и генерирует документы всех групп
(`Degree`: бакалавриат 4 курс ПИиКН/КНиС, магистратура 2 курс МДА/ТРПС):
шаблоны из `src/main/resources/templates/<группа>`, результат
в `build/classes/java/main/documents/<группа>` (при запуске из IDEA путь тот же —
это корень classpath), имя файла начинается с ФИО студента.
Удобный способ запуска: `./gradlew run`.
У некоторых групп список шаблонов закомментирован в `Degree.java` —
раскомментируй нужные строки, чтобы они тоже генерировались.
Если каких-то данных в базе нет, ключевое слово останется в документе,
а в консоль будет выведено предупреждение.

## Легаси: онтология через Protege
Прежний способ наполнения (Protege + Cellfie + SPARQL) заменён базой данных.
Из онтологии остался только файл данных `src/main/resources/main_students_all.owl`
(именно его читает `OntologyMigrator` при каждом запуске); TBox-схема теперь живёт
в changelog Liquibase, правила Cellfie и инструкция по Protege удалены (доступны в истории git).

## Запуск из IDEA
1. Открой папку проекта и импортируй его как Gradle-проект: в дереве проекта дважды кликни
   `build.gradle` -> "Open as Gradle Project" -> Trust Project. Дождись окончания импорта
   (справа появится панель Gradle, зависимости скачаются в фоне).
2. Убедиcь, что PostgreSQL запущен (`docker compose up -d` либо свой сервер).
3. Запусти `src/main/java/ru/nsu/fit/chernyavtseva/department/Main.java` (или `./gradlew run`) —
   IDEA сама создаст конфигурацию запуска Gradle с правильным classpath.

Если запускаешь старой конфигурацией (например, оставшейся от Maven-версии),
добавь в Run Configuration -> Modify options -> VM options:
`-Ddb.url=jdbc:postgresql://localhost:5432/department -Ddb.user=postgres -Ddb.password=postgres`
и замени classpath на Gradle-овский (`build/classes/java/main`; ресурсы — `src/main/resources`).

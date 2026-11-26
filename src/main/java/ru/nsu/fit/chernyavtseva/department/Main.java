package ru.nsu.fit.chernyavtseva.department;

import ru.nsu.fit.chernyavtseva.department.complete_documents.DocumentGenerator;
import ru.nsu.fit.chernyavtseva.department.db.Database;
import ru.nsu.fit.chernyavtseva.department.db.OntologyMigrator;
import ru.nsu.fit.chernyavtseva.department.query_model.QueryExecutor;

import java.io.IOException;

/**
 * Точка входа: при каждом запуске перестраивает наполнение базы из онтологии
 * ({@link OntologyMigrator#migrate}) и генерирует документы для всех групп
 * студентов ({@link Degree#all()}), читая данные из базы данных.
 */
public class Main {
    private static final Degree[] DEGREES = Degree.all();


    public static void main(String[] args) throws IOException {
        try (Database database = Database.open()) {
            System.out.println(OntologyMigrator.migrate(database));
            QueryExecutor executor = new QueryExecutor(database);
            for (Degree degree : DEGREES) {
                DocumentGenerator.generate(degree, executor.findStudents(degree));
            }
        }
    }
}

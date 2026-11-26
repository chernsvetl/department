package ru.nsu.fit.chernyavtseva.department;

import ru.nsu.fit.chernyavtseva.department.complete_documents.DocumentGenerator;
import ru.nsu.fit.chernyavtseva.department.db.DataExporter;
import ru.nsu.fit.chernyavtseva.department.db.Database;
import ru.nsu.fit.chernyavtseva.department.db.OntologyMigrator;
import ru.nsu.fit.chernyavtseva.department.query_model.QueryExecutor;

import java.io.IOException;

/**
 * Точка входа: генерирует документы для всех групп студентов ({@link Degree#all()}),
 * читая данные из базы данных (раньше они читались из онтологии через SPARQL).
 */
public class Main {
    private static final Degree[] DEGREES = Degree.all();


    public static void main(String[] args) throws IOException {
        try (Database database = Database.open()) {
            OntologyMigrator.migrate(database);
            DataExporter.exportToFile(database);
            QueryExecutor executor = new QueryExecutor(database);
            for (Degree degree : DEGREES) {
                DocumentGenerator.generate(degree, executor.findStudents(degree));
            }
        }
    }
}

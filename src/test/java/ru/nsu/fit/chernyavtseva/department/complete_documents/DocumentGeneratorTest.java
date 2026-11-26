package ru.nsu.fit.chernyavtseva.department.complete_documents;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.nsu.fit.chernyavtseva.department.Degree;
import ru.nsu.fit.chernyavtseva.department.TestPostgres;
import ru.nsu.fit.chernyavtseva.department.db.Database;
import ru.nsu.fit.chernyavtseva.department.query_model.QueryExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Заполнение .docx-шаблонов значениями из БД: вместо ключевых слов подставляются
 * данные студента из таблиц, а кафедра и заведующий — из таблицы department.
 */
class DocumentGeneratorTest {

    private static final List<String> KEYWORDS = List.of(
            "имяСтудентаР", "имяСтудентаИ", "группаСтудента", "темаВКР", "местоПрактики",
            "полноеНаименованиеМестаПрактики", "наименованиеОрганизации", "приказПрактика",
            "имяРуководителяОтНГУ", "должностьВНГУ", "имяРуководителяОтОрганизации",
            "должностьВОрганизации", "имяДляПодписи", "имяРуководителяВКР",
            "должностьРуководителяКраткоВКР", "обучСтудОбрПадеж", "бакДатаРук");

    @BeforeAll
    static void prepareDatabase() {
        TestPostgres.createDatabase("documents");
        TestPostgres.migrateOntology();
    }

    @Test
    void fillsTemplatesWithDatabaseValues(@TempDir Path outDir) throws Exception {
        List<Map<String, String>> students;
        try (Database database = Database.open()) {
            students = new QueryExecutor(database).findStudents(Degree.all()[0]);
        }
        assertFalse(students.isEmpty());

        DocumentGenerator.generate(Degree.all()[0], students.subList(0, 1), outDir);

        Path individualTask = outDir.resolve("Авдеев_Данил_Сергеевич_Прил 1_ИЗ на практику_Бакалавриат_ПИиКН_8 семестр.docx");
        assertTrue(Files.exists(individualTask), "шаблон 'Прил 1' должен быть заполнен");
        String text = text(individualTask);

        assertTrue(text.contains("Авдеев Данил Сергеевич"), "ФИО в именительном падеже");
        assertTrue(text.contains("Авдеева Данила Сергеевича"), "ФИО в родительном падеже");
        assertTrue(text.contains("20208"), "номер группы");
        assertTrue(text.contains("Пальчунов Дмитрий Евгеньевич"), "руководитель ВКР");
        assertTrue(text.contains("общей информатики"), "кафедра");
        assertTrue(text.contains("09.02.2024 №8-ас"), "приказ о направлении на практику");
        assertTrue(text.contains("Трегубов Артём Сергеевич"), "руководитель практики от НГУ");

        for (String keyword : KEYWORDS) {
            assertFalse(text.contains(keyword), "в документе осталось ключевое слово: " + keyword);
        }
    }

    @Test
    void generatesAllTemplatesForOneStudent(@TempDir Path outDir) throws Exception {
        List<Map<String, String>> students;
        try (Database database = Database.open()) {
            students = new QueryExecutor(database).findStudents(Degree.all()[0]);
        }

        DocumentGenerator.generate(Degree.all()[0], students.subList(0, 1), outDir);

        try (Stream<Path> files = Files.list(outDir)) {
            long generated = files.count();
            assertTrue(generated >= 12 && generated <= 18,
                    "для одного студента должно заполняться большинство шаблонов, создано: " + generated);
        }
    }

    private static String text(Path docx) throws Exception {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(docx))) {
            for (var paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append('\n');
            }
            for (var table : document.getTables()) {
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        for (var paragraph : cell.getParagraphs()) {
                            text.append(paragraph.getText()).append('\n');
                        }
                    }
                }
            }
        }
        return text.toString();
    }
}

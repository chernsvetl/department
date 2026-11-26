package ru.nsu.fit.chernyavtseva.department.complete_documents;

import org.apache.poi.xwpf.usermodel.*;
import ru.nsu.fit.chernyavtseva.department.Degree;
import ru.nsu.fit.chernyavtseva.department.complete_documents.type.core.DocumentTemplate;
import ru.nsu.fit.chernyavtseva.department.complete_documents.type.ReplacementCreator;
import ru.nsu.fit.chernyavtseva.department.util.PathUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Заполняет .docx-шаблоны: проходит по всем параграфам и таблицам документа и заменяет
 * ключевые слова значениями из строки студента ({@link DocumentTemplate#replacements()}).
 * Готовые файлы складываются в target/classes/documents/<dir Degree> с префиксом "ФИО студента_".
 */
public class DocumentGenerator {

    private static final String TEMPLATES_DIR = "/templates";
    private static final String DOCS_DIR = "documents";

    public static void generate(Degree degree, List<Map<String, String>> students) {
        generate(degree, students, PathUtil.newResourceDir(DOCS_DIR, degree.dir()));
    }

    public static void generate(Degree degree, List<Map<String, String>> students, Path outDirPath) {
        List<DocumentTemplate> templates = Arrays.asList(degree.toGenerate());

        for (Map<String, String> student : students) {
            for (DocumentTemplate template : templates) {
                Path inPath = PathUtil.resource(TEMPLATES_DIR, degree.dir(), template.fileName());
                generateOne(inPath, outDirPath, template, student);
            }
        }
    }

    private static void generateOne(Path inPath, Path outDirPath, DocumentTemplate toGenerate, Map<String, String> student) {
        Map<String, String> studentReplacements = studentReplacements(toGenerate.replacements(), student);
        if (!toGenerate.generateFor(studentReplacements)) {
            return;
        }
        String prefix = student.get(toGenerate.prefixVarName()).replace(' ', '_');
        Set<String> unfilled = new TreeSet<>();
        for (String keyword : toGenerate.replacements().keySet()) {
            if (!studentReplacements.containsKey(keyword)) {
                unfilled.add(keyword);
            }
        }
        Set<String> leftInDocument = new TreeSet<>();
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(inPath))) {
            // replace in paragraphs
            for (XWPFParagraph xwpfParagraph : doc.getParagraphs()) {
                for (XWPFRun xwpfRun : xwpfParagraph.getRuns()) {
                    replaceInRun(xwpfRun, studentReplacements, unfilled, leftInDocument);
                }
            }

            // replace in tables
            for (XWPFTable tbl : doc.getTables()) {
                for (XWPFTableRow row : tbl.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            for (XWPFRun r : p.getRuns()) {
                                replaceInRun(r, studentReplacements, unfilled, leftInDocument);
                            }
                        }
                    }
                }
            }

            Path outPath = outDirPath.resolve(prefix + "_" + toGenerate.fileName());
            try (OutputStream out = Files.newOutputStream(outPath)) {
                doc.write(out);
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!leftInDocument.isEmpty()) {
            System.out.println("Нет данных в базе для документа \"" + prefix + "_" + toGenerate.fileName()
                    + "\", ключевые слова остались незаполненными: " + String.join(", ", leftInDocument));
        }
    }

    private static Map<String, String> studentReplacements(Map<String, ReplacementCreator> creators, Map<String, String> student) {
        Map<String, String> studentReplacements = new HashMap<>();
        for (Map.Entry<String, ReplacementCreator> entry : creators.entrySet()) {
            String replacement = entry.getValue().replacement(student);
            if (replacement != null) {
                studentReplacements.put(entry.getKey(), replacement);
            }
        }
        return studentReplacements;
    }

    private static void replaceInRun(XWPFRun xwpfRun, Map<String, String> replacements, Set<String> unfilled, Set<String> leftInDocument) {
        int nChunks = xwpfRun.getCTR().getTArray().length;
        for (int i = 0; i < nChunks; i++) {
            String text = xwpfRun.getText(i);
            if (text == null) {
                continue;
            }
            String replaced = replaced(text, replacements);
            xwpfRun.setText(replaced, i);
            for (Iterator<String> it = unfilled.iterator(); it.hasNext(); ) {
                String keyword = it.next();
                if (replaced.contains(keyword)) {
                    leftInDocument.add(keyword);
                    it.remove();
                }
            }
        }
    }

    private static String replaced(String original, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            original = original.replace(entry.getKey(), entry.getValue());
        }
        return original;
    }
}

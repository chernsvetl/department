package com.backend.service;

import com.backend.dto.ApplicationSubmitRequest;
import com.backend.dto.ApplicationSubmitResponse;
import com.backend.dto.FormFieldSpec;
import com.backend.dto.FormSection;
import org.apache.jena.sparql.exec.http.UpdateExecHTTP;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    @Value("${fuseki.update.endpoint}")
    private String updateEndpoint;

    @Value("${ontology.base-namespace}")
    private String ns;

    /** Команда переноса онтологии; пусто — используется gradlew migrateOntology проекта department. */
    @Value("${DEPARTMENT_MIGRATE_CMD:}")
    private String migrateCmd;

    /** Корень проекта department (для запуска gradlew); относительный путь — от рабочего каталога бэкенда. */
    @Value("${DEPARTMENT_PROJECT_DIR:../../../..}")
    private String projectDir;

    private final FormSchemaService formSchemaService;
    private final DocumentFillService documentFillService;
    private final EmailService emailService;
    private final TaskScheduler taskScheduler;

    public ApplicationService(FormSchemaService formSchemaService,
                               DocumentFillService documentFillService,
                               EmailService emailService,
                               TaskScheduler taskScheduler) {
        this.formSchemaService = formSchemaService;
        this.documentFillService = documentFillService;
        this.emailService = emailService;
        this.taskScheduler = taskScheduler;
    }

    /**
     * Saves a submitted student form into the ontology (Fuseki via SPARQL UPDATE).
     *
     * Uses form-schema.yaml (via FormSchemaService) to determine for each property:
     *   - how to write it (datatype → literal, object → URI)
     *   - triple direction (reversed: true → value is subject, entity is object)
     *
     * The individual's URI local name is taken from the field marked mainName: true in the schema
     * (spaces removed). Falls back to a random suffix if that field has no value.
     */
    public ApplicationSubmitResponse submitStudent(ApplicationSubmitRequest req, String schemaType, String studentEmail) {
        Map<String, String> values = req.getValues();
        var schema = formSchemaService.getSchema(schemaType);

        // Determine individual local name from the mainName field
        String localName = schema.getSections().stream()
                .flatMap(s -> s.getFields().stream())
                .filter(FormFieldSpec::isMainName)
                .map(f -> values.get(f.getPropUri()))
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.replace(" ", ""))
                .findFirst()
                .orElseGet(() -> UUID.randomUUID().toString().replace("-", "").substring(0, 8));

        String studentUri = ns + localName;

        StringBuilder sb = new StringBuilder("INSERT DATA {\n");

        // Student individual typed as the chosen degree
        appendUri(sb, studentUri, RDF_TYPE, ns + req.getDegree());

        // Write each filled property according to schema metadata
        for (FormSection section : schema.getSections()) {
            for (FormFieldSpec field : section.getFields()) {
                String val = values.get(field.getPropUri());
                if (val == null || val.isBlank()) continue;

                if ("datatype".equals(field.getType())) {
                    appendLiteral(sb, studentUri, field.getPropUri(), val);
                } else {
                    if (field.isReversed()) {
                        appendUri(sb, val, field.getPropUri(), studentUri);
                    } else {
                        // e.g. <studentUri> на_НГУ_практике_у <supervisorUri>
                        appendUri(sb, studentUri, field.getPropUri(), val);
                    }
                }
            }
        }

        sb.append("}");
        String sparql = sb.toString();
        log.debug("SPARQL UPDATE:\n{}", sparql);

        UpdateRequest update = UpdateFactory.create(sparql);
        UpdateExecHTTP.newBuilder()
                .endpoint(updateEndpoint)
                .update(update)
                .build()
                .execute();

        log.info("Application saved: uri={}", studentUri);

        // данные уже в Fuseki — сразу переносим их в PostgreSQL кафедры (department)
        taskScheduler.schedule(this::triggerDepartmentMigration, Instant.now());

        if (!"student".equals(schemaType)) {
            return new ApplicationSubmitResponse(studentUri, "SUBMITTED");
        }

        // Generate documents and email them to the student
        final String finalStudentUri = studentUri;
        try {
            byte[] zip = documentFillService.generateZip(finalStudentUri);
            emailService.sendDocuments(studentEmail, zip);
            log.info("Documents sent to {}", studentEmail);
        } catch (Exception e) {
            log.error("Failed to generate/send documents for {}: {}", finalStudentUri, e.getMessage(), e);
        }

        // Schedule reminder in 30 minutes
        taskScheduler.schedule(
            () -> {
                try {
                    emailService.sendReminder(studentEmail);
                    log.info("Reminder sent to {}", studentEmail);
                } catch (Exception e) {
                    log.error("Reminder failed for {}: {}", studentEmail, e.getMessage(), e);
                }
            },
            Instant.now().plusSeconds(30 * 60)
        );

        return new ApplicationSubmitResponse(studentUri, "SUBMITTED");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Запускает перенос онтологии в PostgreSQL кафедры: migrateOntology проекта department
     * читает граф Fuseki, сохраняет его копию в файл онтологии (main_students_all.owl)
     * и перезаливает данные в базу — сразу после отправки формы, без перезапуска Main.
     * Ошибка не влияет на отправку — данные уже в Fuseki,
     * перенос можно повторить вручную командой ./gradlew migrateOntology.
     */
    private void triggerDepartmentMigration() {
        try {
            Path root = Path.of(projectDir);
            if (!root.isAbsolute()) {
                root = Path.of(System.getProperty("user.dir")).resolve(root).normalize();
            }
            List<String> command = new ArrayList<>();
            if (migrateCmd == null || migrateCmd.isBlank()) {
                // абсолютный путь к gradlew — относительный ProcessBuilder иногда не находит
                command.add(root.resolve("gradlew").toAbsolutePath().toString());
                command.add("-q");
                command.add("migrateOntology");
            } else {
                command.addAll(Arrays.asList(migrateCmd.split("\\s+")));
            }
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(root.toFile());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int code = process.waitFor();
            if (code == 0) {
                log.info("Онтология перенесена в PostgreSQL кафедры: {}", output.isBlank() ? "ok" : output);
            } else {
                log.warn("Перенос онтологии завершился с кодом {}: {}", code, output);
            }
        } catch (Exception e) {
            log.warn("Не удалось запустить перенос онтологии ('{}'): {}", migrateCmd, e.getMessage());
        }
    }

    private void appendUri(StringBuilder sb, String subject, String pred, String object) {
        sb.append("  <").append(subject).append("> ")
          .append("<").append(pred).append("> ")
          .append("<").append(object).append("> .\n");
    }

    private void appendLiteral(StringBuilder sb, String subject, String pred, String value) {
        sb.append("  <").append(subject).append("> ")
          .append("<").append(pred).append("> ")
          .append('"').append(escapeLiteral(value)).append("\"@ru .\n");
    }

    private String escapeLiteral(String v) {
        return v.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

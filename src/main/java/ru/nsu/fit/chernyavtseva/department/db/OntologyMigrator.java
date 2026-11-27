package ru.nsu.fit.chernyavtseva.department.db;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Перенос данных из онтологии кафедры в базу данных.
 *
 * <p>Главный источник онтологии — граф Fuseki системы сбора заполнений
 * (поднимается через src/main/ontology/docker-compose.yml; эндпоинт Graph Store Protocol
 * задаётся параметром ontology.fuseki в db.properties, по умолчанию
 * http://localhost:3030/ontology/data). После успешного чтения граф сохраняется в файл
 * онтологии (параметр ontology.file, по умолчанию src/main/java/ru/nsu/fit/chernyavtseva/
 * department/ontology/main_students_all.owl) — файл всегда остаётся синхронной копией графа
 * и работает как фолбэк, если Fuseki недоступен. Если файла нет — classpath-ресурс
 * main_students_all.owl из того же пакета.
 *
 * <p>Выполняется при каждом запуске Main: таблицы очищаются ({@link DbWriter#clearAll()}) и заново
 * наполняются данными онтологии, поэтому наполнение базы всегда совпадает с онтологией.
 *
 * <p>Читает RDF/XML напрямую (без Jena): individuals онтологии превращаются в строки таблиц
 * person/thesis/student/practice, объектные свойства — в внешние ключи.
 */
public final class OntologyMigrator {

    private static final String ONTOLOGY_RESOURCE = "/ru/nsu/fit/chernyavtseva/department/ontology/main_students_all.owl";
    private static final String CONFIG_FILE = "/db.properties";
    private static final String FUSEKI_PROPERTY = "ontology.fuseki";
    private static final String DEFAULT_FUSEKI = "http://localhost:3030/ontology/data";
    private static final String FILE_PROPERTY = "ontology.file";
    private static final String DEFAULT_ONTOLOGY_FILE =
            "src/main/java/ru/nsu/fit/chernyavtseva/department/ontology/main_students_all.owl";
    private static final Duration FUSEKI_TIMEOUT = Duration.ofSeconds(5);

    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String OWL = "http://www.w3.org/2002/07/owl#";
    private static final String ONTOLOGY = "http://www.semanticweb.org/oleyn/ontologies/2022/4/кафедра#";
    private static final String DEFAULT_DEPARTMENT = "общей информатики";
    private static final String BACHELOR = "Бакалавриат";
    private static final String MASTER = "Магистратура";
    private static final int BACHELOR_COURSE = 4;
    private static final int MASTER_COURSE = 2;
    private static final String THESIS = "ВКР";
    private static final String DEPARTMENT = "Кафедра";
    private static final Set<String> PERSON_TYPES = Set.of(
            "Руководитель_ВКР", "Соруководитель_ВКР", "Консультант", "Руководитель_от_НГУ",
            "Руководитель_от_организации", "Рецензент");
    private static final Map<String, PersonField> PERSON_FIELDS = Map.ofEntries(
            Map.entry("Должность_руководителя_ВКР", PersonField.THESIS_POSITION),
            Map.entry("Должность_руководителя_ВКР_кратко", PersonField.THESIS_POSITION_SHORT),
            Map.entry("Ученая_степень_руководителя_ВКР", PersonField.ACADEMIC_DEGREE),
            Map.entry("Должность", PersonField.POSITION),
            Map.entry("Должность_в_НГУ", PersonField.NGSU_POSITION),
            Map.entry("Должность_в_организации", PersonField.ORGANIZATION_POSITION),
            Map.entry("ФИО_для_подписи", PersonField.SIGNATURE_NAME),
            Map.entry("ДатаБакРук", PersonField.BACHELOR_DATE),
            Map.entry("ДатаБакКнис", PersonField.BACHELOR_KNIS_DATE),
            Map.entry("ДатаМагМДА", PersonField.MASTER_MDA_DATE),
            Map.entry("ДатаМагТРПС", PersonField.MASTER_TRPS_DATE));

    private OntologyMigrator() {
    }

    public static void main(String[] args) throws IOException {
        try (Database database = Database.open()) {
            if (args.length > 0) {
                System.out.println(migrate(database, Path.of(args[0])));
            } else {
                System.out.println(migrate(database));
            }
        }
    }

    /**
     * Переносит данные из онтологии в базу данных — используется при каждом запуске Main
     * и системой сбора после отправки формы. База очищается и наполняется заново.
     * Источники по порядку: граф Fuseki (после чтения сохраняется в файл онтологии),
     * файл по пути ontology.file, classpath-ресурс main_students_all.owl.
     */
    public static String migrate(Database database) throws IOException {
        String fuseki = fusekiEndpoint();
        if (fuseki != null) {
            byte[] graph;
            try (InputStream ontology = fetchFuseki(fuseki)) {
                graph = ontology.readAllBytes();
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Онтология Fuseki (" + fuseki + ") недоступна: " + message(e)
                        + " — читаю файл онтологии");
                graph = null;
            }
            if (graph != null) {
                saveOntologyFile(graph);
                try (InputStream ontology = new ByteArrayInputStream(graph)) {
                    return migrate(database, ontology, fuseki);
                }
            }
        }
        Path file = ontologyFile();
        if (file != null) {
            try (InputStream ontology = Files.newInputStream(file)) {
                return migrate(database, ontology, file.toString());
            } catch (NoSuchFileException e) {
                System.out.println("Файла онтологии нет: " + file.toAbsolutePath()
                        + " — читаю " + ONTOLOGY_RESOURCE + " из ресурсов");
            }
        }
        InputStream resource = OntologyMigrator.class.getResourceAsStream(ONTOLOGY_RESOURCE);
        if (resource == null) {
            throw new IllegalArgumentException("Онтология не найдена в classpath: " + ONTOLOGY_RESOURCE);
        }
        try (InputStream ontology = resource) {
            return migrate(database, ontology, "classpath:" + ONTOLOGY_RESOURCE);
        }
    }

    /**
     * Сохраняет граф Fuseki (RDF/XML) в файл онтологии — файл остаётся синхронной копией
     * графа и фолбэком на случай, когда Fuseki недоступен. Ошибка записи не мешает переносу:
     * в консоль выводится предупреждение.
     */
    private static void saveOntologyFile(byte[] graph) {
        Path file = ontologyFile();
        if (file == null) {
            return;
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.write(file, graph);
            System.out.println("Граф Fuseki сохранён в файл онтологии " + file);
        } catch (IOException e) {
            System.out.println("Не удалось сохранить граф Fuseki в " + file.toAbsolutePath()
                    + ": " + message(e));
        }
    }

    /**
     * Путь к файлу онтологии: системное свойство ontology.file, переменная окружения
     * ONTOLOGY_FILE, параметр ontology.file в db.properties либо путь по умолчанию
     * (main_students_all.owl в пакете ontology). Относительный путь разрешается от рабочего каталога.
     */
    private static Path ontologyFile() {
        String value = configValue(FILE_PROPERTY);
        Path path = value == null || value.isBlank() ? Path.of(DEFAULT_ONTOLOGY_FILE) : Path.of(value.trim());
        return path.isAbsolute() ? path : Path.of("").toAbsolutePath().resolve(path);
    }

    /** Значение параметра: системное свойство, затем переменная окружения, затем db.properties. */
    private static String configValue(String property) {
        String value = System.getProperty(property);
        if (value == null) {
            value = System.getenv(property.toUpperCase(Locale.ROOT).replace('.', '_'));
        }
        if (value != null) {
            return value;
        }
        try (InputStream in = OntologyMigrator.class.getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                Properties config = new Properties();
                config.load(in);
                return config.getProperty(property);
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /** Переносит данные из .owl-файла по указанному пути. */
    public static String migrate(Database database, Path ontology) throws IOException {
        if (!Files.isRegularFile(ontology)) {
            throw new IllegalArgumentException("Файл онтологии не найден: " + ontology.toAbsolutePath());
        }
        try (InputStream in = Files.newInputStream(ontology)) {
            return migrate(database, in, ontology.toString());
        }
    }

    /**
     * Эндпоинт Graph Store Protocol Fuseki: системное свойство ontology.fuseki,
     * переменная окружения ONTOLOGY_FUSEKI или параметр ontology.fuseki в db.properties
     * (по умолчанию http://localhost:3030/ontology/data). Пустое значение или off отключает Fuseki.
     */
    private static String fusekiEndpoint() {
        String value = configValue(FUSEKI_PROPERTY);
        if (value == null) {
            return DEFAULT_FUSEKI;
        }
        String endpoint = value.trim();
        return endpoint.isEmpty() || "off".equalsIgnoreCase(endpoint) ? null : endpoint;
    }

    /** Выгружает граф онтологии из Fuseki в формате RDF/XML. */
    private static InputStream fetchFuseki(String endpoint) throws IOException, InterruptedException {
        String separator = endpoint.contains("?") ? "&" : "?";
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + separator + "default"))
                .header("Accept", "application/rdf+xml")
                .timeout(FUSEKI_TIMEOUT)
                .GET()
                .build();
        HttpResponse<InputStream> response = HttpClient.newBuilder()
                .connectTimeout(FUSEKI_TIMEOUT)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static String message(Exception e) {
        String text = e.getMessage();
        return text == null || text.isBlank() ? e.getClass().getSimpleName() : text;
    }

    private static String migrate(Database database, InputStream ontology, String source) throws IOException {
        Map<String, Individual> individuals = read(ontology);
        DepartmentData data = new DepartmentData(departmentName(individuals), departmentHead(individuals));
        Map<String, PersonData> persons = persons(individuals, data);
        Map<String, ThesisData> theses = theses(individuals, data);
        linkTheses(individuals, persons, theses);
        int[] students = students(individuals, data, persons, theses);
        try (DbWriter writer = new DbWriter(database)) {
            writer.clearAll();
            writer.saveAll(data);
            writer.commit();
        }
        return "Данные онтологии (" + source + ") перенесены в базу данных " + database.url()
                + ": руководителей и рецензентов " + persons.size()
                + ", тем ВКР " + theses.size()
                + ", студентов " + students[0] + " (бакалавров " + students[1] + ", магистрантов " + students[2] + ")"
                + ", практик " + students[3];
    }

    private record Individual(String iri, Set<String> types, Map<String, List<String>> literals, Map<String, List<String>> links) {

        String literal(String property) {
            return first(literals.get(property));
        }

        String link(String property) {
            return first(links.get(property));
        }

        List<String> linksTo(String property) {
            return links.getOrDefault(property, List.of());
        }

        private static String first(List<String> values) {
            return values == null || values.isEmpty() ? null : values.get(0);
        }
    }

    private static Map<String, Individual> read(InputStream ontology) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(ontology);
            Map<String, Individual> individuals = new LinkedHashMap<>();
            // individual кодируются двумя способами: <owl:NamedIndividual rdf:about=...> (Protege)
            // и <rdf:Description rdf:about=...> с вложенным <rdf:type rdf:resource="owl#NamedIndividual">
            // (выгрузка Fuseki/Jena). Схема и классы в rdf:Description не имеют нужных типов
            // и отфильтровываются дальше сами.
            collect(document.getElementsByTagNameNS(OWL, "NamedIndividual"), individuals);
            collect(document.getElementsByTagNameNS(RDF, "Description"), individuals);
            return individuals;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Не удалось прочитать онтологию " + ontology + ": " + e.getMessage(), e);
        }
    }

    private static void collect(NodeList elements, Map<String, Individual> individuals) {
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String iri = local(element.getAttributeNS(RDF, "about"));
            if (iri.isEmpty() || individuals.containsKey(iri)) {
                continue;
            }
            Set<String> types = new LinkedHashSet<>();
            Map<String, List<String>> literals = new HashMap<>();
            Map<String, List<String>> links = new HashMap<>();
            NodeList properties = element.getChildNodes();
            for (int j = 0; j < properties.getLength(); j++) {
                if (!(properties.item(j) instanceof Element property)) {
                    continue;
                }
                String name = property.getLocalName();
                if (RDF.equals(property.getNamespaceURI()) && "type".equals(name)) {
                    types.add(local(property.getAttributeNS(RDF, "resource")));
                } else if (ONTOLOGY.equals(property.getNamespaceURI())) {
                    Map<String, List<String>> target = property.hasAttributeNS(RDF, "resource") ? links : literals;
                    String value = property.hasAttributeNS(RDF, "resource")
                            ? local(property.getAttributeNS(RDF, "resource"))
                            : property.getTextContent();
                    target.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
                }
            }
            individuals.put(iri, new Individual(iri, types, literals, links));
        }
    }

    private static String local(String iri) {
        return iri.substring(Math.max(iri.lastIndexOf('#'), iri.lastIndexOf('/')) + 1);
    }

    private static String departmentName(Map<String, Individual> individuals) {
        for (Individual individual : individuals.values()) {
            if (individual.types().contains(DEPARTMENT)) {
                String name = individual.iri().replace('_', ' ');
                return name.startsWith("кафедра ") ? name.substring("кафедра ".length()) : name;
            }
        }
        return DEFAULT_DEPARTMENT;
    }

    private static String departmentHead(Map<String, Individual> individuals) {
        for (Individual individual : individuals.values()) {
            if (individual.types().contains(DEPARTMENT)) {
                return individual.literal("Заведующий_кафедры");
            }
        }
        return null;
    }

    private static Map<String, PersonData> persons(Map<String, Individual> individuals, DepartmentData data) {
        Set<String> mentionedByStudents = new HashSet<>();
        for (Individual individual : individuals.values()) {
            mentionedByStudents.addAll(individual.linksTo("на_НГУ_практике_у"));
            mentionedByStudents.addAll(individual.linksTo("на_орг_практике_у"));
            mentionedByStudents.addAll(individual.linksTo("пишет_вкр_у"));
        }
        Map<String, PersonData> persons = new HashMap<>();
        for (Individual individual : individuals.values()) {
            if (individual.types().stream().noneMatch(PERSON_TYPES::contains)) {
                continue;
            }
            boolean named = !Text.isBlank(individual.literal("ФИО"));
            if (!named && individual.links().isEmpty() && !mentionedByStudents.contains(individual.iri())) {
                continue;
            }
            String fullName = named ? individual.literal("ФИО") : nameFromIri(individual.iri());
            if (Text.isBlank(fullName)) {
                continue;
            }
            PersonData person = data.person(fullName);
            for (Map.Entry<String, PersonField> field : PERSON_FIELDS.entrySet()) {
                person.set(field.getValue(), individual.literal(field.getKey()));
            }
            persons.put(individual.iri(), person);
        }
        return persons;
    }

    private static String nameFromIri(String iri) {
        return iri.replaceFirst("_\\d+$", "")
                .replaceAll("(?<=[a-zа-яё])(?=[A-ZА-ЯЁ])|(?<=[A-ZА-ЯЁ])(?=[A-ZА-ЯЁ][a-zа-яё])", " ");
    }

    private static Map<String, ThesisData> theses(Map<String, Individual> individuals, DepartmentData data) {
        Map<String, ThesisData> theses = new HashMap<>();
        for (Individual individual : individuals.values()) {
            if (individual.types().contains(THESIS) && !Text.isBlank(individual.literal("Тема"))) {
                theses.put(individual.iri(), data.thesis(individual.literal("Тема")));
            }
        }
        return theses;
    }

    private static void linkTheses(Map<String, Individual> individuals, Map<String, PersonData> persons, Map<String, ThesisData> theses) {
        for (Individual individual : individuals.values()) {
            PersonData person = persons.get(individual.iri());
            if (person == null) {
                continue;
            }
            for (String iri : individual.linksTo("согласовывает")) {
                if (theses.containsKey(iri)) {
                    theses.get(iri).supervisor(person);
                }
            }
            for (String iri : individual.linksTo("руководит_с")) {
                if (theses.containsKey(iri)) {
                    theses.get(iri).coSupervisor(person);
                }
            }
            for (String iri : individual.linksTo("консультирует")) {
                if (theses.containsKey(iri)) {
                    theses.get(iri).consultant(person);
                }
            }
            for (String iri : individual.linksTo("рецензирует")) {
                if (theses.containsKey(iri)) {
                    theses.get(iri).reviewer(person);
                }
            }
        }
    }

    private static int[] students(Map<String, Individual> individuals, DepartmentData data,
                                  Map<String, PersonData> persons, Map<String, ThesisData> theses) {
        int total = 0;
        int bachelors = 0;
        int masters = 0;
        int practices = 0;
        for (Individual individual : individuals.values()) {
            String degree = degree(individual);
            if (degree == null || Text.isBlank(individual.literal("ФИО"))) {
                continue;
            }
            int course = BACHELOR.equals(degree) ? BACHELOR_COURSE : MASTER_COURSE;
            StudentData student = data.addStudent(degree, course, individual.literal("ФИО"),
                    individual.literal("группа"), individual.literal("Профиль_обучения"));
            if (theses.containsKey(individual.link("защищает"))) {
                student.setThesis(theses.get(individual.link("защищает")));
            }
            PracticeData practice = student.practice();
            practice.setPlace(individual.literal("Место_прохождения_практики"));
            practice.setFullPlace(individual.literal("Место_практики_полное_наименование"));
            practice.setOrganizationName(individual.literal("Наименование_организации"));
            practice.setOrder(individual.literal("Приказ_на_прохождение_практики"));
            practice.setNgsuSupervisor(persons.get(ngsuSupervisorIri(individual)));
            practice.setOrganizationSupervisor(persons.get(individual.link("на_орг_практике_у")));
            total++;
            if (BACHELOR.equals(degree)) {
                bachelors++;
            } else {
                masters++;
            }
            if (!practice.isEmpty()) {
                practices++;
            }
        }
        return new int[]{total, bachelors, masters, practices};
    }

    /**
     * Руководитель практики от НГУ: классическое свойство онтологии на_НГУ_практике_у,
     * а для заполнений из веб-формы — пишет_вкр_у (форма отправляет
     * руководителя этим свойством).
     */
    private static String ngsuSupervisorIri(Individual individual) {
        String iri = individual.link("на_НГУ_практике_у");
        return iri != null ? iri : individual.link("пишет_вкр_у");
    }

    private static String degree(Individual individual) {
        if (individual.types().contains(BACHELOR)) {
            return BACHELOR;
        }
        return individual.types().contains(MASTER) ? MASTER : null;
    }
}

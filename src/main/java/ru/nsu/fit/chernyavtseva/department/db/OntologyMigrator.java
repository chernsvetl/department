package ru.nsu.fit.chernyavtseva.department.db;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Разовый перенос данных из онтологии кафедры (src/main/resources/main_students_all.owl) в базу данных.
 *
 * <p>Запуск: java ru.nsu.fit.chernyavtseva.department.db.OntologyMigrator [путь к .owl].
 * Читает RDF/XML напрямую (без Jena): individuals онтологии превращаются в строки таблиц
 * person/thesis/student/practice, объектные свойства — в внешние ключи.
 */
public final class OntologyMigrator {

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
        Path ontology = Path.of(args.length > 0 ? args[0] : "src/main/resources/main_students_all.owl");
        try (Database database = Database.open()) {
            System.out.println(migrate(database, ontology));
        }
    }

    /**
     * Переносит данные из онтологии по умолчанию (src/main/resources/main_students_all.owl)
     * в базу данных — используется при запуске Main.
     */
    public static String migrate(Database database) throws IOException {
        return migrate(database, Path.of("src/main/resources/main_students_all.owl"));
    }

    public static String migrate(Database database, Path ontology) throws IOException {
        if (!Files.isRegularFile(ontology)) {
            throw new IllegalArgumentException("Файл онтологии не найден: " + ontology.toAbsolutePath());
        }
        Map<String, Individual> individuals = read(ontology);
        DepartmentData data = new DepartmentData(departmentName(individuals), departmentHead(individuals));
        Map<String, PersonData> persons = persons(individuals, data);
        Map<String, ThesisData> theses = theses(individuals, data);
        linkTheses(individuals, persons, theses);
        int[] students = students(individuals, data, persons, theses);
        try (DbWriter writer = new DbWriter(database)) {
            writer.saveAll(data);
            writer.commit();
        }
        return "Данные онтологии перенесены в базу данных " + database.url()
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

    private static Map<String, Individual> read(Path ontology) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(ontology.toFile());
            Map<String, Individual> individuals = new LinkedHashMap<>();
            NodeList elements = document.getElementsByTagNameNS(OWL, "NamedIndividual");
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                String iri = local(element.getAttributeNS(RDF, "about"));
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
            return individuals;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Не удалось прочитать онтологию " + ontology + ": " + e.getMessage(), e);
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
            practice.setNgsuSupervisor(persons.get(individual.link("на_НГУ_практике_у")));
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

    private static String degree(Individual individual) {
        if (individual.types().contains(BACHELOR)) {
            return BACHELOR;
        }
        return individual.types().contains(MASTER) ? MASTER : null;
    }
}

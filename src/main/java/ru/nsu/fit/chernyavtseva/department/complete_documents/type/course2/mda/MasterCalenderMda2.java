package ru.nsu.fit.chernyavtseva.department.complete_documents.type.course2.mda;

import ru.nsu.fit.chernyavtseva.department.complete_documents.type.ReplacementCreator;
import ru.nsu.fit.chernyavtseva.department.complete_documents.type.core.DocumentTemplate;
import java.util.HashMap;
import java.util.Map;

public record MasterCalenderMda2() implements DocumentTemplate {
    private static final Map<String, ReplacementCreator> DOC_FIELD_TO_SOLUTION;

    static {
        DOC_FIELD_TO_SOLUTION = new HashMap<>();
    }

    @Override
    public Map<String, ReplacementCreator> replacements() {
        return DOC_FIELD_TO_SOLUTION;
    }

    @Override
    public String fileName() {
        return "Магистратура_Задание на ВКР_МАД_(с_соруководителем).docx";
    }
}

package ru.nsu.fit.chernyavtseva.department.complete_documents.type.course4.piikn;

import ru.nsu.fit.chernyavtseva.department.complete_documents.type.ReplacementCreator;
import ru.nsu.fit.chernyavtseva.department.complete_documents.type.core.DocumentTemplate;

import java.util.HashMap;
import java.util.Map;

public record BachelorListLiteraturePikn() implements DocumentTemplate {
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
        return "Список_использованных_источников_и_литературы_бакалавриат_ПИиКН.docx";
    }
}

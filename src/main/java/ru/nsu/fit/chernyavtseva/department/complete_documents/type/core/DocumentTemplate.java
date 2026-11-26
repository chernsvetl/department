package ru.nsu.fit.chernyavtseva.department.complete_documents.type.core;

import ru.nsu.fit.chernyavtseva.department.complete_documents.type.ReplacementCreator;

import java.util.Map;

/**
 * Один заполняемый документ: имя .docx-шаблона ({@link #fileName()}) и словарь
 * "ключевое слово в шаблоне -> откуда взять значение" ({@link #replacements()}).
 */
public interface DocumentTemplate {

    /**
     * template field -> replacer
     */
    Map<String, ReplacementCreator> replacements();

    /**
     * Original template file name.
     */
    String fileName();

    /**
     * Solution var name that will be used as prefix for generated document
     */
    default String prefixVarName() {
        return "фио_студента";
    }

    default boolean generateFor(Map<String, String> studentReplacements) {
        return true;
    }
}


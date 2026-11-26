package ru.nsu.fit.chernyavtseva.department.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextTest {

    @Test
    void cleanRemovesNonBreakingSpacesAndTrimsEdges() {
        assertEquals("доцент", Text.clean("\u00A0доцент "));
    }

    @Test
    void cleanKeepsNullAsNull() {
        assertNull(Text.clean(null));
    }

    @Test
    void cleanTurnsBlanksIntoEmptyString() {
        assertEquals("", Text.clean("   "));
    }

    @Test
    void nameCollapsesInnerWhitespace() {
        assertEquals("Пальчунов Дмитрий Евгеньевич", Text.name("Пальчунов  Дмитрий\u00A0Евгеньевич "));
    }

    @Test
    void isBlankDetectsEmptyAndNull() {
        assertTrue(Text.isBlank(""));
        assertTrue(Text.isBlank(null));
        assertTrue(Text.isBlank("  "));
        assertFalse(Text.isBlank("доцент"));
    }
}

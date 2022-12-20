package fr.enimaloc.jircd.message;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaskTest {

    @Test
    void toRegex() {
        assertEquals("\\Q?\\E", new Mask("\\?").toRegex());
        assertEquals(".", new Mask("?").toRegex());
        assertEquals(".*", new Mask("*").toPattern().toString());
        assertEquals("\\Qj\\E\\Qi\\E\\Qr\\E\\Qc\\E\\Qd\\E", new Mask("jircd").toPattern().toString());
    }

    @Test
    void toPattern() {
        assertTrue(patternEquals(Pattern.compile("\\Q?\\E"), new Mask("\\?").toPattern()));
        assertTrue(patternEquals(Pattern.compile("."), new Mask("?").toPattern()));
        assertTrue(patternEquals(Pattern.compile(".*"), new Mask("*").toPattern()));
        assertTrue(patternEquals(Pattern.compile("\\Qj\\E\\Qi\\E\\Qr\\E\\Qc\\E\\Qd\\E"), new Mask("jircd").toPattern()));
    }

    boolean patternEquals(Pattern p1, Pattern p2) {
        return p1.pattern().equals(p2.pattern()) && p1.flags() == p2.flags() && p1.toString().equals(p2.toString());
    }
}
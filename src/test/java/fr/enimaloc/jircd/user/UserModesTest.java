package fr.enimaloc.jircd.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserModesTest {

    @Test
    void invisible() {
        UserModes userModes = new UserModes();
        assertFalse(userModes.invisible());
        userModes.invisible(true);
        assertTrue(userModes.invisible());
        assertEquals("i", userModes.toString());
        assertEquals("", userModes.prefix());
    }

    @Test
    void oper() {
        UserModes userModes = new UserModes();
        assertFalse(userModes.oper());
        userModes.oper(true);
        assertTrue(userModes.oper());
        assertEquals("o", userModes.toString());
        assertEquals("@", userModes.prefix());
    }

    @Test
    void registered() {
        UserModes userModes = new UserModes();
        assertFalse(userModes.registered());
        userModes.registered(true);
        assertTrue(userModes.registered());
        assertEquals("r", userModes.toString());
        assertEquals("", userModes.prefix());
    }

    @Test
    void localOper() {
        UserModes userModes = new UserModes();
        assertFalse(userModes.localOper());
        userModes.localOper(true);
        assertTrue(userModes.localOper());
        assertEquals("O", userModes.toString());
        assertEquals("@", userModes.prefix());
    }

    @Test
    void wallops() {
        UserModes userModes = new UserModes();
        assertFalse(userModes.wallops());
        userModes.wallops(true);
        assertTrue(userModes.wallops());
        assertEquals("w", userModes.toString());
        assertEquals("", userModes.prefix());
    }
}
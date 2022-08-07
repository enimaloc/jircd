package fr.enimaloc.jircd.commands.optional;

import fr.enimaloc.jircd.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class AwayCommandTest extends OptionalCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void awayTest() {
        connections[0].createUser("bob", "Mobbye Plav");

        User bob = server.users().get(0);
        assumeTrue(bob != null);

        connections[0].send("AWAY :I'm away");
        assertArrayEquals(new String[]{
                ":jircd-host 306 bob :You have been marked as being away"
        }, connections[0].awaitMessage());
        assertTrue(bob.away().isPresent());
    }

    @Test
    void unawayTest() {
        connections[0].createUser("bob", "Mobbye Plav");

        User bob = server.users().get(0);
        assumeTrue(bob != null);
        bob.away("I'm away");
        assumeTrue(bob.away().isPresent());

        connections[0].send("AWAY");
        assertArrayEquals(new String[]{
                ":jircd-host 305 bob :You are no longer marked as being away"
        }, connections[0].awaitMessage());
        assertTrue(bob.away().isEmpty());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
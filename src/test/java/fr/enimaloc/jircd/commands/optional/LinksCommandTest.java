package fr.enimaloc.jircd.commands.optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class LinksCommandTest extends OptionalCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void linksTest() {
        connections[0].createUser("bob", "Mobbye Plav");

        connections[0].send("LINKS");
        assertArrayEquals(new String[]{
                ":jircd-host 364 bob * jircd-host :0 jircd is a lightweight IRC server written in Java.",
                ":jircd-host 365 bob * :End of /LINKS list"
        }, connections[0].awaitMessage(2));
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
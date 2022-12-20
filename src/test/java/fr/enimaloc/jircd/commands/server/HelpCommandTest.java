package fr.enimaloc.jircd.commands.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class HelpCommandTest extends ServerCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void helpNoSubjectTest() {
        connections[0].send("HELP");
        assertArrayEquals(new String[]{
                ":jircd-host 524 bob :No help available on this topic",
        }, connections[0].awaitMessage());
    }

    @Test
    void helpTest() {
        connections[0].send("HELP subject");
        assertArrayEquals(new String[]{
                ":jircd-host 524 bob subject :No help available on this topic",
        }, connections[0].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
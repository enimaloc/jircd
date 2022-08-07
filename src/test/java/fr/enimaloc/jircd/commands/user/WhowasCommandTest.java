package fr.enimaloc.jircd.commands.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class WhowasCommandTest extends UserCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void whowasTest() {
        connections[0].send("WHOWAS joe");
        assertArrayEquals(new String[]{
                ":jircd-host 406 bob :There was no such nickname",
                ":jircd-host 369 bob joe :End of WHOWAS"
        }, connections[0].awaitMessage(2));
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
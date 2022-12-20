package fr.enimaloc.jircd.commands.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class ConnectCommandTest extends ServerCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void connectErrorTest() {
        connections[0].send("CONNECT one");
        assertArrayEquals(new String[]{
                ":jircd-host 400 bob CONNECT :Not supported yet"
        }, connections[0].awaitMessage());

        connections[0].send("CONNECT one two");
        assertArrayEquals(new String[]{
                ":jircd-host 400 bob CONNECT :Not supported yet"
        }, connections[0].awaitMessage());

        connections[0].send("CONNECT one two three");
        assertArrayEquals(new String[]{
                ":jircd-host 400 bob CONNECT :Not supported yet"
        }, connections[0].awaitMessage());
    }

    @Test
    @Disabled("Not supported yet")
    void connectOneArgumentTest() {
        connections[0].send("CONNECT one");
    }

    @Test
    @Disabled("Not supported yet")
    void connectTwoArgumentTest() {
        connections[0].send("CONNECT one two");
    }

    @Test
    @Disabled("Not supported yet")
    void connectThreeArgumentTest() {
        connections[0].send("CONNECT one two three");
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.SocketBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class PingCommandTest extends ConnectionCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void pingNoParamsTest() {
        assertArrayEquals(
                new String[]{":jircd-host 461 @127.0.0.1 PING :Not enough parameters"},
                connections[0].send("PING", 1)
        );
    }

    @Test
    void pingWithParamsTest() {
        assertArrayEquals(
                new String[]{"PONG Hello"},
                connections[0].send("PING Hello", 1)
        );
    }

    @Test
    void pingWithTrailingTest() {
        assertArrayEquals(
                new String[]{"PONG Hello "},
                connections[0].send("PING :Hello ", 1)
        );
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
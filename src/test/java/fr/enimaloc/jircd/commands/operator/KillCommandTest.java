package fr.enimaloc.jircd.commands.operator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class KillCommandTest extends OperatorCommandBase {

    @BeforeEach
    void setUp() {
        init();
        addConnections(1);
        connections[0].createUser("bob", "bobby", "Mobbye Plav");
        connections[1].createUser("john", "John Doe");
        connections[0].send("JOIN #kill");
        connections[0].ignoreMessage(3);

        connections[1].send("JOIN #kill");
        connections[1].ignoreMessage(3);
        connections[0].ignoreMessage();
    }

    @Test
    void killTest() {
        assumeTrue(server.users().get(0) != null);
        server.users().get(0).modes().oper(true);
        connections[0].send("KILL john :Stop spamming");
        assertArrayEquals(new String[]{
                ":john QUIT :Quit: Killed (bob (Stop spamming))"
        }, connections[0].awaitMessage());
        assertArrayEquals(new String[]{
                "Closing Link: jircd-host (Killed (bob (Stop spamming)))"
        }, connections[1].awaitMessage());
        assertArrayEquals(SOCKET_CLOSE, connections[1].awaitMessage());
    }

    @Test
    void killLocalOperTest() {
        assumeTrue(server.users().get(0) != null);
        server.users().get(0).modes().localOper(true);
        connections[0].send("KILL john :Stop spamming");
        assertArrayEquals(new String[]{
                ":john QUIT :Quit: Killed (bob (Stop spamming))"
        }, connections[0].awaitMessage());
        assertArrayEquals(new String[]{
                "Closing Link: jircd-host (Killed (bob (Stop spamming)))"
        }, connections[1].awaitMessage());
        assertArrayEquals(SOCKET_CLOSE, connections[1].awaitMessage());
    }

    @Test
    void killUnknownTest() {
        assumeTrue(server.users().get(0) != null);
        server.users().get(0).modes().localOper(true);
        connections[0].send("KILL x :Stop spamming");
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
        assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
    }

    @Test
    void killNotOperTest() {
        connections[0].send("KILL john :Stop spamming");
        assertArrayEquals(new String[]{
                ":jircd-host 481 bob :Permission Denied- You're not an IRC operator"
        }, connections[0].awaitMessage());
        assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
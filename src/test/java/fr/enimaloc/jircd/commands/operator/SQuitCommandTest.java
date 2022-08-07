package fr.enimaloc.jircd.commands.operator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class SQuitCommandTest extends OperatorCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void quitTest() {
        connections[0].createUser("bob", "Mobbye Plav");

        assumeTrue(server.users().get(0) != null);
        server.users().get(0).modes().oper(true);

        connections[0].send("SQUIT notASrv :I'm out");
        assertArrayEquals(new String[]{
                ":jircd-host 402 bob notASrv :No such server"
        }, connections[0].awaitMessage());
    }

    @Test
    void quitNotOperTest() {
        connections[0].createUser("bob", "Mobbye Plav");

        connections[0].send("SQUIT notASrv :I'm out");
        assertArrayEquals(new String[]{
                ":jircd-host 481 bob :Permission Denied- You're not an IRC operator"
        }, connections[0].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
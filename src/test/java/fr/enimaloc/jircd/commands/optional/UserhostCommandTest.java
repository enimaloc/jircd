package fr.enimaloc.jircd.commands.optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class UserhostCommandTest extends OptionalCommandBase {

    @BeforeEach
    void setUp() {
        init();
        connections[0].createUser("bob", "bobby", "Mobbye Plav");
    }

    @Test
    void userhostWithOneParametersTest() {
        addConnections(1);
        connections[1].createUser("john", "John Doe");

        connections[0].send("USERHOST john");
        assertArrayEquals(new String[]{
                ":jircd-host 302 bob :john+127.0.0.1"
        }, connections[0].awaitMessage());
    }

    @Test
    void userhostWithTwoParametersTest() {
        addConnections(2);
        connections[1].createUser("john", "John Doe");
        connections[2].createUser("fred", "Fred Bloggs");

        connections[0].send("USERHOST john fred");
        assertArrayEquals(new String[]{
                ":jircd-host 302 bob :john+127.0.0.1 fred+127.0.0.1"
        }, connections[0].awaitMessage());
    }

    @Test
    void userhostWithThreeParametersTest() {
        addConnections(3);
        connections[1].createUser("john", "John Doe");
        connections[2].createUser("fred", "Fred Bloggs");
        connections[3].createUser("tommy", "Tommy Atkins");

        connections[0].send("USERHOST john fred tommy");
        assertArrayEquals(new String[]{
                ":jircd-host 302 bob :john+127.0.0.1 fred+127.0.0.1 tommy+127.0.0.1"
        }, connections[0].awaitMessage());
    }

    @Test
    void userhostWithFourParametersTest() {
        addConnections(4);
        connections[1].createUser("john", "John Doe");
        connections[2].createUser("fred", "Fred Bloggs");
        connections[3].createUser("tommy", "Tommy Atkins");
        connections[4].createUser("ann", "Ann Yonne");

        connections[0].send("USERHOST john fred tommy ann");
        assertArrayEquals(new String[]{
                ":jircd-host 302 bob :john+127.0.0.1 fred+127.0.0.1 tommy+127.0.0.1 ann+127.0.0.1"
        }, connections[0].awaitMessage());
    }

    @Test
    void userhostWithFiveParametersTest() {
        addConnections(5);
        connections[1].createUser("john", "John Doe");
        connections[2].createUser("fred", "Fred Bloggs");
        connections[3].createUser("tommy", "Tommy Atkins");
        connections[4].createUser("ann", "Ann Yonne");
        connections[5].createUser("ratman", "Doug Rattmann");

        connections[0].send("USERHOST john fred tommy ann ratman");
        assertArrayEquals(new String[]{
                ":jircd-host 302 bob :john+127.0.0.1 fred+127.0.0.1 tommy+127.0.0.1 ann+127.0.0.1 ratman+127.0.0.1"
        }, connections[0].awaitMessage());
    }

    @Test
    void userhostOperatorTest() {
        addConnections(2);
        connections[1].createUser("john", "John Doe");
        assumeTrue(server.users().get(1) != null);
        server.users().get(1).modes().oper(true);
        connections[2].createUser("fred", "Fred Bloggs");

        connections[0].send("USERHOST john fred");
        assertArrayEquals(new String[]{
                ":jircd-host 302 bob :john*+127.0.0.1 fred+127.0.0.1"
        }, connections[0].awaitMessage());
    }

    @Test
    void userhostAwayTest() {
        addConnections(2);
        connections[1].createUser("john", "John Doe");
        assumeTrue(server.users().get(1) != null);
        server.users().get(1).away("Away!");
        connections[2].createUser("fred", "Fred Bloggs");

        connections[0].send("USERHOST john fred");
        assertArrayEquals(new String[]{
                ":jircd-host 302 bob :john-127.0.0.1 fred+127.0.0.1"
        }, connections[0].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
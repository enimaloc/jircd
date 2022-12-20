package fr.enimaloc.jircd.commands.optional;

import fr.enimaloc.jircd.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class WallOpsCommandTest extends OptionalCommandBase {

    @BeforeEach
    void setUp() {
        init();
        addConnections(2);
        connections[0].createUser("bob", "Mobbye Plav");

        assumeTrue(getUser("bob").isPresent());
        User bob = getUser("bob").get();
        bob.info().setOper(baseSettings.operators().get(0));
        assumeTrue(bob.modes().oper());

        connections[1].createUser("john", "John Doe");

        assumeTrue(getUser("john").isPresent());
        User john = getUser("john").get();
        john.modes().wallops(true);
        assumeFalse(john.modes().oper());
        assumeTrue(john.modes().wallops());

        connections[2].createUser("jane", "Jane Doe");

        assumeTrue(getUser("jane").isPresent());
        User jane = getUser("jane").get();
        assumeFalse(jane.modes().oper());
        assumeFalse(jane.modes().wallops());
    }

    @Test
    void wallopsTest() {
        connections[0].send("WALLOPS :Hello world!");
        assertArrayEquals(new String[]{
                ":bob WALLOPS :Hello world!"
        }, connections[0].awaitMessage());
        assertArrayEquals(new String[]{
                ":bob WALLOPS :Hello world!"
        }, connections[1].awaitMessage());
        assertArrayEquals(EMPTY_ARRAY, connections[2].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
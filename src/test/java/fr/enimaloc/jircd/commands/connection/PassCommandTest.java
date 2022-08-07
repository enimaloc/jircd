package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.SocketBase;
import fr.enimaloc.jircd.user.UserInfo;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class PassCommandTest extends ConnectionCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void passTest() {
        connections[0].send("PASS " + baseSettings.pass);
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
        assumeTrue(waitFor(() -> server.users().size() > 0));
        UserInfo info = server.users().get(0).info();
        assertTrue(waitFor(info::passwordValid));

        assertEquals("127.0.0.1", info.host());
        assertNull(info.username());
        assertNull(info.nickname());
        assertNull(info.realName());
        assertFalse(info.canRegistrationBeComplete());
    }

    @Test
    void noParamTest() {
        assertArrayEquals(new String[]{
                ":jircd-host 461 @127.0.0.1 PASS :Not enough parameters"
        }, connections[0].send("PASS", 1));
        assumeTrue(waitFor(() -> server.users().size() > 0));
        assertFalse(server.users().get(0).info().passwordValid());
    }

    @Test
    void incorrectPassTest() {
        String passwd = getRandomString(new Random().nextInt(9) + 1);
        assumeFalse(baseSettings.pass.equals(passwd));
        assertArrayEquals(new String[]{
                ":jircd-host 464 @127.0.0.1 :Password incorrect"
        }, connections[0].send("PASS " + passwd, 1));
        assumeTrue(waitFor(() -> server.users().size() > 0));
        assertFalse(server.users().get(0).info().passwordValid());
    }

    @Test
    void alreadyRegisteredPassTest() {
        connections[0].createUser("john", "John Doe");
        connections[0].send("PASS " + baseSettings.pass);
        assertArrayEquals(new String[]{
                ":jircd-host 462 john :You may not reregister"
        }, connections[0].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
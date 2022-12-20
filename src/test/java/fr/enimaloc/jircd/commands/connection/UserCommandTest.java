package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.SocketBase;
import fr.enimaloc.jircd.user.UserInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class UserCommandTest extends ConnectionCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void userTest() {
        baseSettings.pass().ifPresent(pass -> connections[0].send("PASS " + pass));
        connections[0].send("USER bobby 0 * :Mobbye Plav");
        assumeTrue(waitFor(() -> server.users().size() > 0));
        UserInfo info = server.users().get(0).info();
        assertTrue(waitFor(() -> info.username() != null && info.realName() != null));
        assertEquals("bobby", info.username());
        assertEquals("Mobbye Plav", info.realName());
        assertTrue(info.passwordValid());

        assertEquals("127.0.0.1", info.host());
        assertNull(info.nickname());
        assertFalse(info.canRegistrationBeComplete());
    }

    @Test
    void alreadyRegisteredUserTest() {
        baseSettings.pass().ifPresent(pass -> connections[0].send("PASS " + pass));
        connections[0].send("NICK bob");
        connections[0].send("USER bobby 0 * :Mobby Plav");
        connections[0].ignoreMessage(6 + attrLength + baseSettings.motd().length);
        connections[0].send("USER bob 0 * :Mobba Plav");
        assertArrayEquals(new String[]{
                ":jircd-host 462 bob :You may not reregister"
        }, connections[0].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
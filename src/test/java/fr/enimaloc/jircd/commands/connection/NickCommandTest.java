package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.SocketBase;
import fr.enimaloc.jircd.user.UserInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;
import utils.ListUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class NickCommandTest extends ConnectionCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void nickTest() {
        connections[0].send("PASS " + baseSettings.pass);

        connections[0].send("NICK bob");
        assumeTrue(waitFor(() -> server.users().size() > 0));
        UserInfo info = server.users().get(0).info();
        assertTrue(waitFor(info::passwordValid));
        assumeTrue(waitFor(() -> info.nickname() != null));
        assertEquals("bob", info.nickname());
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());

        assertEquals("127.0.0.1", info.host());
        assertNull(info.username());
        assertNull(info.realName());
        assertFalse(info.canRegistrationBeComplete());
    }

    @Test
    void incorrectLengthNickTest() {
        connections[0].send("PASS " + baseSettings.pass);
        String nickname = getRandomString(50);
        connections[0].send("NICK " + nickname);
        assertArrayEquals(new String[]{
                ":jircd-host 432 @127.0.0.1 " + nickname + " :Erroneus nickname"
        }, connections[0].awaitMessage());
    }

    @Test
    void incorrectNickTest() {
        connections[0].send("PASS " + baseSettings.pass);
        String nickname = getRandomString(7, 160, 255, i -> true);
        connections[0].send("NICK " + nickname);
        assertArrayEquals(new String[]{
                ":jircd-host 432 @127.0.0.1 " + nickname + " :Erroneus nickname"
        }, connections[0].awaitMessage());
    }

    @Test
    void duplicateNickTest() {
        addConnections(1);
        connections[0].send("PASS " + baseSettings.pass);
        connections[1].send("PASS " + baseSettings.pass);
        connections[0].send("NICK dup");
        connections[1].ignoreMessage();
        connections[1].send("NICK dup");
        assertArrayEquals(new String[]{
                ":jircd-host 433 @127.0.0.1 dup :Nickname is already in use"
        }, connections[1].awaitMessage());
    }

    @Test
    void unsafeNickWithSafenetTest() {
        connections[0].send("PASS " + baseSettings.pass);
        String nick = ListUtils.getRandom(baseSettings.unsafeNickname);
        connections[0].send("NICK " + nick);

        assumeTrue(waitFor(() -> server.users().size() > 0));
        UserInfo info = server.users().get(0).info();
        assertTrue(waitFor(info::passwordValid));
        assumeTrue(waitFor(() -> info.nickname() != null));
        assertEquals(nick, info.nickname());
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());

        assertEquals("127.0.0.1", info.host());
        assertNull(info.username());
        assertNull(info.realName());
        assertFalse(info.canRegistrationBeComplete());
    }

    @Test
    void unsafeNickWithUnsafenetTest() {
        connections[0].send("PASS " + baseSettings.pass);
        String nick = ListUtils.getRandom(baseSettings.unsafeNickname);

        assumeTrue(waitFor(() -> server.users().size() > 0));
        UserInfo info = server.users().get(0).info();
        info.setHost("255.255.255.255");

        connections[0].send("NICK " + nick);
        assertArrayEquals(new String[]{
                ":jircd-host 432 @255.255.255.255 " + nick + " :Erroneus nickname"
        }, connections[0].awaitMessage());
        assertNull(info.nickname());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
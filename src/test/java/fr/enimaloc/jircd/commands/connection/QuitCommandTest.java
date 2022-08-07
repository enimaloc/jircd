package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.SocketBase;
import fr.enimaloc.jircd.user.User;
import fr.enimaloc.jircd.user.UserState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class QuitCommandTest extends ConnectionCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void quitTest() {
        assumeTrue(waitFor(() -> server.users().size() == 1));
        User user = server.users().get(0);

        connections[0].send("QUIT", 1);
        assertTrue(server.users().isEmpty());
        assertEquals(UserState.DISCONNECTED, user.state());
    }

    @Test
    void quitWithReasonTest() {
        assumeTrue(waitFor(() -> server.users().size() == 1));
        User user = server.users().get(0);

        connections[0].send("QUIT :Bye", 1);
        assertTrue(waitFor(() -> server.users().isEmpty()));
        assertEquals(UserState.DISCONNECTED, user.state());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
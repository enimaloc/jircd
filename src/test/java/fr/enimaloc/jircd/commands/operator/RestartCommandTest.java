package fr.enimaloc.jircd.commands.operator;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class RestartCommandTest extends OperatorCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    // FIXME: 30/07/2022 Thread not interrupted on TravisCI
    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "Thread not interrupt on TravisCI")
    void restartTest() {
        connections[0].createUser("bob", "Mobbye Plav");

        assumeTrue(server.users().get(0) != null);
        server.users().get(0).modes().oper(true);

        connections[0].send("RESTART");
        assertTrue(waitFor(server::isShutdown, 5, TimeUnit.MINUTES));
        assertTrue(waitFor(server::isInterrupted, 1, TimeUnit.MINUTES));
        assertTrue(waitFor(() -> !connections[0].testConnection(baseSettings.port)));
        assertTrue(waitFor(() -> {
            try {
                connections[0] = createConnection();
                return true;
            } catch (ConnectException e) {
                return false;
            } catch (IOException e) {
                fail(e);
                return false;
            }
        }, 1, TimeUnit.MINUTES));
        connections[0].createUser("bob", "Mobbye Plav");
    }

    @Test
    void restartNotOperTest() {
        connections[0].createUser("bob", "Mobbye Plav");

        connections[0].send("RESTART");
        assertArrayEquals(new String[]{
                ":jircd-host 481 bob :Permission Denied- You're not an IRC operator"
        }, connections[0].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
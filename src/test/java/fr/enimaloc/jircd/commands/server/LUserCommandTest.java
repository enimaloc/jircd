package fr.enimaloc.jircd.commands.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class LUserCommandTest extends ServerCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void luserTest() {
        connections[0].send("LUSER");
        assertArrayEquals(new String[]{
                ":jircd-host 251 bob :There are 1 users and 0 invisibles on 1 servers",
                ":jircd-host 252 bob 0 :operator(s) online",
                ":jircd-host 253 bob 0 :unknown connection(s)",
                ":jircd-host 254 bob 0 :channels formed",
                ":jircd-host 255 bob :I have 1 clients and 1 servers"
        }, connections[0].awaitMessage(5));
    }

    @Test
    void luserTestWithUnknown() {
        addConnections(1);

        connections[0].send("LUSER");
        assertArrayEquals(new String[]{
                ":jircd-host 251 bob :There are 1 users and 0 invisibles on 1 servers",
                ":jircd-host 252 bob 0 :operator(s) online",
                ":jircd-host 253 bob 1 :unknown connection(s)",
                ":jircd-host 254 bob 0 :channels formed",
                ":jircd-host 255 bob :I have 2 clients and 1 servers"
        }, connections[0].awaitMessage(5));
    }

    @Test
    void luserTestWithChannel() {
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(3);

        assumeTrue(server.channels().size() == 1);

        connections[0].send("LUSER");
        assertArrayEquals(new String[]{
                ":jircd-host 251 bob :There are 1 users and 0 invisibles on 1 servers",
                ":jircd-host 252 bob 0 :operator(s) online",
                ":jircd-host 253 bob 0 :unknown connection(s)",
                ":jircd-host 254 bob 1 :channels formed",
                ":jircd-host 255 bob :I have 1 clients and 1 servers"
        }, connections[0].awaitMessage(5));
    }

    @Test
    void luserTestWithInvisible() {
        assumeTrue(waitFor(() -> server.users().size() > 0));
        server.users().get(0).modes().invisible(true);
        assumeTrue(server.users().get(0).modes().invisible());

        connections[0].send("LUSER");
        assertArrayEquals(new String[]{
                ":jircd-host 251 bob :There are 0 users and 1 invisibles on 1 servers",
                ":jircd-host 252 bob 0 :operator(s) online",
                ":jircd-host 253 bob 0 :unknown connection(s)",
                ":jircd-host 254 bob 0 :channels formed",
                ":jircd-host 255 bob :I have 1 clients and 1 servers"
        }, connections[0].awaitMessage(5));
    }

    @Test
    void luserTestWithOper() {
        connections[0].send("OPER " + baseSettings.operators().get(0).username() + " " +
                            baseSettings.operators().get(0).password());
        connections[0].ignoreMessage();
        assumeTrue(waitFor(() -> server.users().size() > 0));
        assumeTrue(server.users().get(0).modes().oper());

        connections[0].send("LUSER");
        assertArrayEquals(new String[]{
                ":jircd-host 251 bob :There are 1 users and 0 invisibles on 1 servers",
                ":jircd-host 252 bob 1 :operator(s) online",
                ":jircd-host 253 bob 0 :unknown connection(s)",
                ":jircd-host 254 bob 0 :channels formed",
                ":jircd-host 255 bob :I have 1 clients and 1 servers"
        }, connections[0].awaitMessage(5));
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
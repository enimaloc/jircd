package fr.enimaloc.jircd.commands.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class WhoCommandTest extends UserCommandBase {

    @BeforeEach
    void setUp() {
        super.init();
    }

    @Test
    void whoWithChannelTest() {
        connections[0].send("JOIN #bob");
        connections[0].awaitMessage(3);

        connections[1].send("JOIN #bob");
        connections[1].awaitMessage(3);
        connections[0].awaitMessage();

        connections[2].send("JOIN #bob");
        connections[2].awaitMessage(3);
        connections[0].awaitMessage();
        connections[1].awaitMessage();

        connections[3].send("JOIN #bob");
        connections[3].awaitMessage(3);
        connections[0].awaitMessage();
        connections[1].awaitMessage();
        connections[2].awaitMessage();

        connections[0].send("WHO #bob");
        assertArrayEquals(new String[]{
                ":jircd-host 352 bob #bob bob 127.0.0.1 jircd-host bob H~ :0 Mobbie Plav",
                ":jircd-host 352 bob #bob fred 127.0.0.1 jircd-host fred H* :0 Fred Bloggs",
                ":jircd-host 352 bob #bob john enimaloc.fr jircd-host john H :0 John Doe",
                ":jircd-host 352 bob #bob jane 127.0.0.1 jircd-host jane G :0 Jane Doe",
                ":jircd-host 315 bob #bob :End of /WHO list"
        }, connections[0].awaitMessage(5));
    }

    @Test
    void whoWhitUserTest() {
        connections[0].send("JOIN #bob");
        connections[0].awaitMessage(3);

        connections[0].send("WHO bob");
        assertArrayEquals(new String[]{
                ":jircd-host 352 bob #bob bob 127.0.0.1 jircd-host bob H~ :0 Mobbie Plav",
                ":jircd-host 315 bob bob :End of /WHO list"
        }, connections[0].awaitMessage(2));

        connections[0].send("WHO fred");
        assertArrayEquals(new String[]{
                ":jircd-host 352 bob * fred 127.0.0.1 jircd-host fred H* :0 Fred Bloggs",
                ":jircd-host 315 bob fred :End of /WHO list"
        }, connections[0].awaitMessage(2));
    }

    @Test
    void whoWithMaskTest() {
        connections[0].send("JOIN #bob");
        connections[0].awaitMessage(3);

        connections[2].send("JOIN #bob");
        connections[2].awaitMessage(3);
        connections[0].awaitMessage();

        connections[0].send("WHO *");
        assertArrayEquals(new String[]{
                ":jircd-host 352 bob #bob bob 127.0.0.1 jircd-host bob H~ :0 Mobbie Plav",
                ":jircd-host 352 bob * fred 127.0.0.1 jircd-host fred H* :0 Fred Bloggs",
                ":jircd-host 352 bob #bob john enimaloc.fr jircd-host john H :0 John Doe",
                ":jircd-host 352 bob * jane 127.0.0.1 jircd-host jane G :0 Jane Doe",
                ":jircd-host 315 bob * :End of /WHO list"
        }, connections[0].awaitMessage(5));
    }

    @AfterEach
    void tearDown() {
        off();
    }

}
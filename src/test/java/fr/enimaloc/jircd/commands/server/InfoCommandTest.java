package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.Constant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class InfoCommandTest extends ServerCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void infoWithUnknownServerTest() {
        connections[0].send("INFO UnknownServer");
        assertArrayEquals(new String[]{
                ":jircd-host 402 bob UnknownServer :No such server"
        }, connections[0].awaitMessage());
    }

    @Test
    void infoTest() {
        connections[0].send("INFO");
        assertArrayEquals(new String[]{
                ":jircd-host 371 :jircd v%s".formatted(Constant.VERSION),
                ":jircd-host 371 :by Antoine <antoine@enimaloc.fr>",
                ":jircd-host 371 :Source code: https://github.com/enimaloc/jircd",
                ":jircd-host 374 :End of /INFO list"
        }, connections[0].awaitMessage(4));
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
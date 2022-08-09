package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.server.ServerSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class AdminCommandTest extends ServerCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void adminWithUnknownServerTest() {
        connections[0].send("ADMIN UnknownServer");
        assertArrayEquals(new String[]{
                ":jircd-host 402 bob UnknownServer :No such server"
        }, connections[0].awaitMessage());
    }

    @Override
    protected ServerSettings.Builder buildSettings() {
        return super.buildSettings().admin(new ServerSettings.Admin(
                "Location 1",
                "Location 2",
                "jircd@local.host"
        ));
    }

    @Test
    void adminTest() {
        connections[0].send("ADMIN");

        assertArrayEquals(new String[]{
                ":jircd-host 256 bob jircd-host :Administrative info",
                ":jircd-host 257 bob :Location 1",
                ":jircd-host 258 bob :Location 2",
                ":jircd-host 259 bob :jircd@local.host"
        }, connections[0].awaitMessage(4));
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.server.ServerSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class MotdCommandTest extends ServerCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void motdWithUnknownServerTest() {
        connections[0].send("MOTD UnknownServer");
        assertArrayEquals(new String[]{
                ":jircd-host 402 bob UnknownServer :No such server"
        }, connections[0].awaitMessage());
    }

    @Test
    void motdWithNoMOTDTest() {
        assumeTrue(server.settings().motd.length == 0);
        connections[0].send("MOTD");
        assertArrayEquals(new String[]{
                ":jircd-host 422 bob :MOTD File is missing"
        }, connections[0].awaitMessage());
    }

    @Test
    void motdTest() throws IOException {
        Path tempFile = Files.createTempFile("motd", ".txt");
        Files.writeString(tempFile, "Custom motd set in temp file");

        setSettings(
                baseSettings.copy(new ServerSettings(tempFile), field -> !field.getName().equals("motd")));

        assumeFalse(server.settings().motd.length == 0);
        connections[0].send("MOTD");
        assertArrayEquals(new String[]{
                ":jircd-host 375 bob :- jircd-host Message of the day - ",
                ":jircd-host 372 bob :Custom motd set in temp file",
                ":jircd-host 376 bob :End of /MOTD command."
        }, connections[0].awaitMessage(3));
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
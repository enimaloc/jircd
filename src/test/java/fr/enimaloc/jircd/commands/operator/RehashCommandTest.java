package fr.enimaloc.jircd.commands.operator;

import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.server.ServerSettings;
import fr.enimaloc.jircd.user.User;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class RehashCommandTest extends OperatorCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void rehashTest() {
        connections[0].createUser("bob", "Mobbye Plav");

        assumeTrue(server.users().get(0) != null);
        server.users().get(0).modes().oper(true);

        // TODO: 28/07/2022 - Add config change here
        //  temp solution : create a file named "settings.toml", and put another value, an hardcoded text is
        //  loaded a start of the test, and don't take settings.toml as base config file.
        ServerSettings temp = new ServerSettings.Builder()
                .host("another-host")
                .build();

        Path path = Path.of("settings.toml");
        if (!Files.exists(path)) {
            temp.saveAs(path);
        }

        assertArrayEquals(new String[]{
                ":%s 382 bob settings.toml :Rehashing".formatted(server.settings().host())
        }, connections[0].send("REHASH", 1));
        connections[0].ignoreMessage();

        assertNotEquals(baseSettings, server.settings());
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rehashNotOperTest() {
        connections[0].createUser("bob", "Mobbye Plav");

        connections[0].send("REHASH");
        assertArrayEquals(new String[]{
                ":jircd-host 481 bob :Permission Denied- You're not an IRC operator"
        }, connections[0].awaitMessage());

        assertEquals(baseSettings, server.settings());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
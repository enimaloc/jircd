package fr.enimaloc.jircd.commands.server;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class TimeCommandTest extends ServerCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void timeWithUnknownServerTest() {
        connections[0].send("TIME UnknownServer");
        assertArrayEquals(new String[]{
                ":jircd-host 402 bob UnknownServer :No such server"
        }, connections[0].awaitMessage());
    }

    @Test
    void timeTest() {
        connections[0].send("TIME");

        assertArrayEquals(new String[]{
                ":jircd-host 391 jircd-host :%s".formatted(
                        DateTimeFormatter.ofPattern("EEEE LLLL dd yyyy - HH:mm O", Locale.ENGLISH)
                                         .format(ZonedDateTime.now()))
        }, connections[0].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
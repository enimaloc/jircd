package fr.enimaloc.jircd.commands.server;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class StatsCommandTest extends ServerCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    @Disabled("No completed")
    void statsCTest() {
        connections[0].send("STATS c");
    }

    @Test
    @Disabled("No completed")
    void statsHTest() {
        connections[0].send("STATS h");
    }

    @Test
    @Disabled("No completed")
    void statsITest() {
        connections[0].send("STATS i");
    }

    @Test
    @Disabled("No completed")
    void statsKTest() {
        connections[0].send("STATS k");
    }

    @Test
    @Disabled("No completed")
    void statsLTest() {
        connections[0].send("STATS l");
    }

    @Test
    void statsMTest() {
        connections[0].send("STATS m");
        assertArrayEquals(new String[]{
                ":jircd-host 212 ADMIN 0",
                ":jircd-host 212 AWAY 0",
                ":jircd-host 212 CONNECT 0",
                ":jircd-host 212 HELP 0",
                ":jircd-host 212 INFO 0",
                ":jircd-host 212 JOIN 0",
                ":jircd-host 212 KICK 0",
                ":jircd-host 212 KILL 0",
                ":jircd-host 212 LINKS 0",
                ":jircd-host 212 LIST 0",
                ":jircd-host 212 LUSER 0",
                ":jircd-host 212 MODE 0",
                ":jircd-host 212 MOTD 0",
                ":jircd-host 212 NAMES 0",
                ":jircd-host 212 NICK 1",
                ":jircd-host 212 NOTICE 0",
                ":jircd-host 212 OPER 0",
                ":jircd-host 212 PART 0",
                ":jircd-host 212 PASS 1",
                ":jircd-host 212 PING 0",
                ":jircd-host 212 PRIVMSG 0",
                ":jircd-host 212 QUIT 0",
                ":jircd-host 212 REHASH 0",
                ":jircd-host 212 RESTART 0",
                ":jircd-host 212 SQUIT 0",
                ":jircd-host 212 STATS 1",
                ":jircd-host 212 TIME 0",
                ":jircd-host 212 TOPIC 0",
                ":jircd-host 212 USER 1",
                ":jircd-host 212 USERHOST 0",
                ":jircd-host 212 VERSION 0",
                ":jircd-host 212 WALLOPS 0",
                ":jircd-host 212 WHO 0",
                ":jircd-host 212 WHOIS 0",
                ":jircd-host 212 WHOWAS 0",
                ":jircd-host 219 M :End of /STATS report"
        }, connections[0].awaitMessage(36));
    }

    @Test
    @Disabled("No completed")
    void statsOTest() {
        connections[0].send("STATS o");
    }

    @Test
    void statsUTest() {
        Pattern pat = Pattern.compile("^:jircd-host 242 :Server Up 0 days 0:[0-5][0-9]:[0-5][0-9]$");

        connections[0].send("STATS u");
        assertTrue(pat.matcher(connections[0].awaitMessage()[0]).matches());
        assertEquals(":jircd-host 219 U :End of /STATS report", connections[0].awaitMessage()[0]);

        assertTrue(waitFor(72, TimeUnit.SECONDS));
        connections[0].send("STATS u");
        assertTrue(pat.matcher(connections[0].awaitMessage()[0]).matches());
        assertEquals(":jircd-host 219 U :End of /STATS report", connections[0].awaitMessage()[0]);
    }

    @Test
    @Disabled("No completed")
    void statsYTest() {
        connections[0].send("STATS y");
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
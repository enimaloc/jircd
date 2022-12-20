package fr.enimaloc.jircd.commands.channel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@CommandClazzTest
class ListCommandTest extends CommandChannelBase {

    @BeforeEach
    void setUp() {
        init();
        addConnections(1);
        connections[1].createUser("john", "John Doe");
    }

    @Test
    void listAllTest() {
        connections[0].send("JOIN #hello,#bonjour,#hey,#bye");
        connections[0].ignoreMessage(3 * 4);
        assumeFalse(server.channels().isEmpty());

        connections[1].send("LIST");
        assertArrayEquals(new String[]{
                ":jircd-host 321 john Channel :Users  Name",
                ":jircd-host 322 john #hello 1 :",
                ":jircd-host 322 john #bonjour 1 :",
                ":jircd-host 322 john #hey 1 :",
                ":jircd-host 322 john #bye 1 :",
                ":jircd-host 323 john :End of /LIST"
        }, connections[1].awaitMessage(6));
    }

    @Test
    void listWithMinimumUserTest() {
        addConnections(3);

        connections[0].send("JOIN #hello,#bonjour,#hey,#bye");
        connections[0].ignoreMessage(3 * 4);

        connections[1].send("JOIN #bonjour,#bye,#chuss");
        connections[1].ignoreMessage(3 * 3);

        connections[2].createUser("fred", "Fred Bloggs");
        connections[2].send("JOIN #hello,#bonjour");
        connections[2].ignoreMessage(3 * 2);

        connections[3].createUser("tommy", "Tommy Atkins");
        connections[3].send("JOIN #hello,#bonjour,#bye,#chuss");
        connections[3].ignoreMessage(3 * 3);
        assumeFalse(server.channels().isEmpty());

        connections[4].createUser("ann", "Ann Yonne");
        connections[4].send("LIST >2");
        assertArrayEquals(new String[]{
                ":jircd-host 321 ann Channel :Users  Name",
                ":jircd-host 322 ann #hello 3 :",
                ":jircd-host 322 ann #bonjour 4 :",
                ":jircd-host 322 ann #bye 3 :",
                ":jircd-host 323 ann :End of /LIST"
        }, connections[4].awaitMessage(5));
    }

    @Test
    void listWithMaximumUserTest() {
        addConnections(3);

        connections[0].send("JOIN #hello,#bonjour,#hey,#bye");
        connections[0].ignoreMessage(3 * 4);

        connections[1].send("JOIN #bonjour,#bye,#chuss");
        connections[1].ignoreMessage(3 * 3);

        connections[2].createUser("fred", "Fred Bloggs");
        connections[2].send("JOIN #hello,#bonjour");
        connections[2].ignoreMessage(3 * 2);

        connections[3].createUser("tommy", "Tommy Atkins");
        connections[3].send("JOIN #hello,#bonjour,#bye,#chuss");
        connections[3].ignoreMessage(3 * 3);
        assumeFalse(server.channels().isEmpty());

        connections[4].createUser("ann", "Ann Yonne");
        connections[4].send("LIST <2");
        assertArrayEquals(new String[]{
                ":jircd-host 321 ann Channel :Users  Name",
                ":jircd-host 322 ann #hey 1 :",
                ":jircd-host 323 ann :End of /LIST"
        }, connections[4].awaitMessage(3));
    }

    @Test
    void listWithMask() {
        connections[0].send("JOIN #42,#azerty,#AZERTY,#1abc2");
        connections[0].ignoreMessage(3 * 4);

        connections[1].send("LIST #[0-9]*");
        assertArrayEquals(new String[]{
                ":jircd-host 321 john Channel :Users  Name",
                ":jircd-host 323 john :End of /LIST"
        }, connections[1].awaitMessage(2));

        connections[1].send("LIST #[0-9][abc]{3}[0-9]");
        assertArrayEquals(new String[]{
                ":jircd-host 321 john Channel :Users  Name",
                ":jircd-host 323 john :End of /LIST"
        }, connections[1].awaitMessage(2));

        connections[1].send("LIST #[a-z]*");
        assertArrayEquals(new String[]{
                ":jircd-host 321 john Channel :Users  Name",
                ":jircd-host 323 john :End of /LIST"
        }, connections[1].awaitMessage(2));

        connections[1].send("LIST #[A-Z]*");
        assertArrayEquals(new String[]{
                ":jircd-host 321 john Channel :Users  Name",
                ":jircd-host 323 john :End of /LIST"
        }, connections[1].awaitMessage(2));

        connections[1].send("LIST #*");
        assertArrayEquals(new String[]{
                ":jircd-host 321 john Channel :Users  Name",
                ":jircd-host 322 john #42 1 :",
                ":jircd-host 322 john #azerty 1 :",
                ":jircd-host 322 john #AZERTY 1 :",
                ":jircd-host 322 john #1abc2 1 :",
                ":jircd-host 323 john :End of /LIST"
        }, connections[1].awaitMessage(6));

        connections[1].send("LIST #");
        assertArrayEquals(new String[]{
                ":jircd-host 321 john Channel :Users  Name",
                ":jircd-host 322 john #42 1 :",
                ":jircd-host 322 john #azerty 1 :",
                ":jircd-host 322 john #AZERTY 1 :",
                ":jircd-host 322 john #1abc2 1 :",
                ":jircd-host 323 john :End of /LIST"
        }, connections[1].awaitMessage(6));

        connections[1].send("LIST 2");
        assertArrayEquals(new String[]{
                ":jircd-host 321 john Channel :Users  Name",
                ":jircd-host 322 john #42 1 :",
                ":jircd-host 322 john #1abc2 1 :",
                ":jircd-host 323 john :End of /LIST"
        }, connections[1].awaitMessage(4));
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
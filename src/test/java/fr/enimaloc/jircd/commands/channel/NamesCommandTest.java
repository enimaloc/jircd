package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.channel.Channel;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class NamesCommandTest extends CommandChannelBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void namesTest() {
        addConnections(1);
        connections[0].send("JOIN #names");
        connections[0].ignoreMessage(4);

        connections[1].createUser("john", "John Doe");
        connections[1].send("NAMES #names");
        assertArrayEquals(new String[]{
                ":jircd-host 353 john = #names :~bob",
                ":jircd-host 366 john #names :End of /NAMES list"
        }, connections[1].awaitMessage(2));
    }

    @Test
    void namesNoArgumentsTest() {
        addConnections(2);
        connections[0].send("JOIN #names");
        connections[0].ignoreMessage(3);

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN #names,#joker");
        connections[1].ignoreMessage(6);

        connections[2].createUser("fred", "Fred Bloggs");
        connections[2].send("NAMES");
        assertArrayEquals(new String[]{
                ":jircd-host 353 fred = #names :~bob john",
                ":jircd-host 366 fred #names :End of /NAMES list",
                ":jircd-host 353 fred = #joker :~john",
                ":jircd-host 366 fred #joker :End of /NAMES list"
        }, connections[2].awaitMessage(4));
    }

    @Test
    void namesNotJoinedSecretChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #names");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#names");
        assumeTrue(channelOpt.isPresent());
        Channel channel = channelOpt.get();
        channel.modes().secret(true);

        connections[1].createUser("john", "John Doe");
        connections[1].send("NAMES #names");
        assertArrayEquals(new String[]{
                ":jircd-host 366 john #names :End of /NAMES list"
        }, connections[1].awaitMessage(1));
    }

    @Test
    void namesJoinedSecretChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #names");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#names");
        assumeTrue(channelOpt.isPresent());
        Channel channel = channelOpt.get();
        channel.modes().secret(true);

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN #names");
        connections[1].ignoreMessage(4);
        connections[1].send("NAMES #names");
        assertArrayEquals(new String[]{
                ":jircd-host 353 john @ #names :~bob john",
                ":jircd-host 366 john #names :End of /NAMES list"
        }, connections[1].awaitMessage(2));
    }

    @Test
    void namesWithInvisibleNotJoinedChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #names");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#names");
        assumeTrue(channelOpt.isPresent());
        server.users().get(0).modes().invisible(true);

        connections[1].createUser("john", "John Doe");
        connections[1].send("NAMES #names");
        assertArrayEquals(new String[]{
                ":jircd-host 353 john = #names :",
                ":jircd-host 366 john #names :End of /NAMES list"
        }, connections[1].awaitMessage(2));
    }

    @Test
    void namesWithInvisibleJoinedChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #names");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#names");
        assumeTrue(channelOpt.isPresent());
        server.users().get(0).modes().invisible(true);

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN #names");
        connections[1].ignoreMessage(4);
        connections[1].send("NAMES #names");
        assertArrayEquals(new String[]{
                ":jircd-host 353 john = #names :~bob john",
                ":jircd-host 366 john #names :End of /NAMES list"
        }, connections[1].awaitMessage(2));
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
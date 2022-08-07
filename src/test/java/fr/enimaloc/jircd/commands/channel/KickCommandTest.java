package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.user.User;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class KickCommandTest extends CommandChannelBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void kickTest() {
        addConnections(1);

        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(3);

        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        connections[1].createUser("fred", "Fred Bloggs");
        connections[1].send("JOIN #jircd");
        connections[1].ignoreMessage(3);
        connections[0].ignoreMessage();

        assumeTrue(channel.users().size() == 2);

        connections[0].send("KICK #jircd fred");
        assertArrayEquals(new String[]{
                ":bob KICK #jircd fred :Kicked by bob"
        }, connections[0].awaitMessage());

        assertArrayEquals(new String[]{
                ":bob KICK #jircd fred :Kicked by bob"
        }, connections[1].awaitMessage());

        assertTrue(waitFor(() -> channel.users().size() == 1, 1, TimeUnit.MINUTES));
    }

    @Test
    void kickReasonTest() {
        addConnections(1);

        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(3);

        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        connections[1].createUser("fred", "Fred Bloggs");
        connections[1].send("JOIN #jircd");
        connections[1].ignoreMessage(3);
        connections[0].ignoreMessage();

        assumeTrue(channel.users().size() == 2);

        connections[0].send("KICK #jircd fred :test");
        assertArrayEquals(new String[]{
                ":bob KICK #jircd fred :test"
        }, connections[0].awaitMessage());

        assertArrayEquals(new String[]{
                ":bob KICK #jircd fred :test"
        }, connections[1].awaitMessage());

        assertTrue(waitFor(() -> channel.users().size() == 1, 1, TimeUnit.MINUTES));
    }

    @Test
    void kickNoPrivsTest() {
        addConnections(1);

        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(3);

        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        connections[1].createUser("fred", "Fred Bloggs");
        connections[1].send("JOIN #jircd");
        connections[1].ignoreMessage(3);
        connections[0].ignoreMessage();

        assumeTrue(channel.users().size() == 2);

        connections[1].send("KICK #jircd bob");
        assertArrayEquals(new String[]{
                ":jircd-host 482 fred #jircd :You're not channel operator"
        }, connections[1].awaitMessage());

        assertEquals(2, channel.users().size());
    }

    @Test
    void kickNoChannelTest() {
        String channel = "#" + getRandomString(7, 128, 255, i -> true, StandardCharsets.ISO_8859_1);
        connections[0].send("KICK " + channel + " fred");
        assertArrayEquals(new String[]{
                ":jircd-host 403 bob " + channel + " :No such channel"
        }, connections[0].awaitMessage());
    }

    @Test
    void kickNotOnChannelTest() {
        connections[0].send("KICK #jircd fred");
        assertArrayEquals(new String[]{
                ":jircd-host 442 bob #jircd :You're not on that channel"
        }, connections[0].awaitMessage());
    }

    @Test
    void kickUserNotInChannelTest() {
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(3);

        connections[0].send("KICK #jircd fred");
        assertArrayEquals(new String[]{
                ":jircd-host 441 bob fred #jircd :They aren't on that channel"
        }, connections[0].awaitMessage());
    }

    @Test
    void kickProtectedUserTest() {
        addConnections(1);

        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(3);

        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        connections[1].createUser("fred", "Fred Bloggs");
        connections[1].send("JOIN #jircd");
        connections[1].ignoreMessage(3);
        connections[0].ignoreMessage();

        Optional<User> userOpt = getUser("fred");
        assertFalse(userOpt.isEmpty());
        User user = userOpt.get();
        channel.prefix(user, Channel.Rank.PROTECTED.prefix + "");

        assumeTrue(channel.users().size() == 2);
        assumeTrue(channel.isRanked(user, Channel.Rank.PROTECTED));

        connections[0].send("KICK #jircd fred");
        assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());

        assertEquals(2, channel.users().size());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
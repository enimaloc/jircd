package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.channel.Channel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@CommandClazzTest
class TopicCommandTest extends CommandChannelBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void noTopicTest() {
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        assertTrue(channel.topic().isEmpty());

        connections[0].send("TOPIC #jircd");
        assertArrayEquals(new String[]{
                ":jircd-host 331 bob #jircd :No topic is set"
        }, connections[0].awaitMessage());
    }

    @Test
    void topicTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        channel.topic(new Channel.Topic("Example topic", server.users().get(0)));
        assertTrue(channel.topic().isPresent());

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN " + channel.name());
        connections[1].ignoreMessage(4);
        connections[1].send("TOPIC " + channel.name());
        Channel.Topic topic = channel.topic().get();
        assertArrayEquals(new String[]{
                ":jircd-host 332 john #jircd :Example topic",
                ":jircd-host 333 john #jircd bob %s".formatted(topic.unixTimestamp())
        }, connections[1].awaitMessage(2));
    }

    @Test
    void topicWithNoParameterTest() {
        connections[0].send("TOPIC");
        assertArrayEquals(new String[]{
                ":jircd-host 461 bob TOPIC :Not enough parameters"
        }, connections[0].awaitMessage());
    }

    @Test
    void topicIncorrectChannelNameTest() {
        String channelName = "#" + getRandomString(7, 128, 255, i -> true, StandardCharsets.ISO_8859_1);
        connections[0].send("TOPIC " + channelName);
        assertArrayEquals(new String[]{
                ":jircd-host 403 bob %s :No such channel".formatted(channelName)
        }, connections[0].awaitMessage());
    }

    @Test
    void topicNotOnChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());

        connections[1].createUser("john", "John Doe");
        connections[1].send("TOPIC #jircd");
        assertArrayEquals(new String[]{
                ":jircd-host 442 john #jircd :You're not on that channel"
        }, connections[1].awaitMessage());
    }

    @Test
    void protectedTopicChangeButNoPermTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(3);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();
        channel.modes().protected0(true);

        assumeFalse(channel.users().isEmpty());
        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN #jircd");
        connections[1].send("TOPIC #jircd :Another topic");
        connections[1].ignoreMessage(3);
        assertArrayEquals(new String[]{
                ":jircd-host 482 john #jircd :You're not channel operator"
        }, connections[1].awaitMessage());
        assertTrue(channel.topic().isEmpty());
    }

    @Test
    void nonProtectedTopicChangeButNoPermTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(3);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        assumeFalse(channel.users().isEmpty());
        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN #jircd");
        connections[1].send("TOPIC #jircd :Another topic");
        connections[1].ignoreMessage(3);
        assertArrayEquals(new String[]{
                ":jircd-host 332 john #jircd :Another topic"
        }, connections[1].awaitMessage());
        assertFalse(channel.topic().isEmpty());
    }

    @Test
    void topicChangeWithPermTest() {
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(3);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        assumeFalse(channel.users().isEmpty());
        connections[0].send("TOPIC #jircd :Another topic");
        assertArrayEquals(new String[]{
                ":jircd-host 332 bob #jircd :Another topic"
        }, connections[0].awaitMessage());
        assertFalse(channel.topic().isEmpty());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
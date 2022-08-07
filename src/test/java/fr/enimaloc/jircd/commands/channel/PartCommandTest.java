package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.channel.Channel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class PartCommandTest extends CommandChannelBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void partTest() {
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        assertFalse(server.users().get(0).channels().isEmpty());
        connections[0].send("PART " + channel.name());
        assertArrayEquals(new String[]{
                ":bob PART #jircd"
        }, connections[0].awaitMessage());
    }

    @Test
    void partWithReason() {
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        assertFalse(server.users().get(0).channels().isEmpty());
        connections[0].send("PART " + channel.name() + " :Bye!");
        assertArrayEquals(new String[]{
                ":bob PART #jircd :Bye!"
        }, connections[0].awaitMessage());
    }

    @Test
    void partMultipleTest() {
        List<Channel> channels = new ArrayList<>();
        addConnections(1);
        for (int i = 0; i < 5; i++) {
            String channelName = "#" + getRandomString(5);
            connections[0].send("JOIN " + channelName);
            connections[0].ignoreMessage(3);
            Optional<Channel> channelOpt = getChannel(channelName);
            assertFalse(channelOpt.isEmpty());
            channels.add(channelOpt.get());
        }

        connections[1].createUser("john", "John Doe");
        connections[1].ignoreMessage(6 + attrLength + baseSettings.motd.length);
        connections[1].send("JOIN " + channels.stream()
                                              .map(Channel::name)
                                              .collect(Collectors.joining(",")));
        connections[1].ignoreMessage(channels.size() * 4);
        assertFalse(server.users().get(1).channels().isEmpty());
        connections[1].send("PART " + channels.stream()
                                              .map(Channel::name)
                                              .collect(Collectors.joining(",")));
        assertArrayEquals(channels.stream().map(c -> ":john PART " + c.name()).toArray(),
                          connections[1].awaitMessage(channels.size()));
        assertTrue(waitFor(() -> server.users().get(1).channels().isEmpty()));
    }

    @Test
    void partWithoutParametersTest() {
        connections[0].send("PART");
        assertArrayEquals(new String[]{
                ":jircd-host 461 bob PART :Not enough parameters"
        }, connections[0].awaitMessage());
    }

    @Test
    void partInvalidChannelNameTest() {
        String channelName = getRandomString(7, 128, 255, i -> true, StandardCharsets.ISO_8859_1);
        connections[0].send("PART " + channelName);
        assertArrayEquals(new String[]{
                ":jircd-host 403 bob %s :No such channel".formatted(channelName)
        }, connections[0].awaitMessage());
    }

    @Test
    void partNotJoinedChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());

        connections[1].createUser("john", "John Doe");
        connections[1].send("PART #jircd");
        assertArrayEquals(new String[]{
                ":jircd-host 442 john #jircd :You're not on that channel"
        }, connections[1].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
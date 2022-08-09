package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.channel.Channel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;
import utils.CommandNameGen;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class JoinCommandTest extends CommandChannelBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void joinChannelTest() {
        connections[0].send("JOIN #jircd");

        assertArrayEquals(new String[]{
                ":bob JOIN #jircd",
                ":jircd-host 353 bob = #jircd :~bob",
                ":jircd-host 366 bob #jircd :End of /NAMES list"
        }, connections[0].awaitMessage(3));

        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();
        assertFalse(channel.users().isEmpty());
    }

    @Test
    void joinWithTopicChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        channel.topic(new Channel.Topic("Example topic", server.users().get(0)));

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN #jircd");
        assertArrayEquals(new String[]{
                ":john JOIN #jircd",
                ":jircd-host 332 john #jircd :Example topic",
                ":jircd-host 353 john = #jircd :~bob john",
                ":jircd-host 366 john #jircd :End of /NAMES list"
        }, connections[1].awaitMessage(4));
        assertEquals(2, channel.users().size());
    }

    @Test
    void joinSecretChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        channel.modes().secret(true);
        assumeTrue(channel.modes().secret());

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN #jircd");
        assertArrayEquals(new String[]{
                ":john JOIN #jircd",
                ":jircd-host 353 john @ #jircd :~bob john",
                ":jircd-host 366 john #jircd :End of /NAMES list"
        }, connections[1].awaitMessage(3));
        assertEquals(2, channel.users().size());
    }

    @Test
    void joinPasswdProtectedChannelWithCorrectPasswdTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        channel.modes().password("complicatedPasswd");
        assumeTrue(channel.modes().password().isPresent());

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN #jircd " + channel.modes().password().get());
        assertArrayEquals(new String[]{
                ":john JOIN #jircd",
                ":jircd-host 353 john = #jircd :~bob john",
                ":jircd-host 366 john #jircd :End of /NAMES list"
        }, connections[1].awaitMessage(3));
        assertEquals(2, channel.users().size());
    }

    @Test
    void joinMultipleChannelTest() {
        addConnections(1);
        List<Channel> channels = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String channelName = "#" + getRandomString(5);
            connections[0].send("JOIN " + channelName);
            connections[0].ignoreMessage(4);
            Optional<Channel> channelOpt = getChannel(channelName);
            assertFalse(channelOpt.isEmpty());
            channels.add(channelOpt.get());
        }
        StringBuilder channelsNameList = new StringBuilder();
        List<String>  expectedOutput   = new ArrayList<>();
        int           j                = 0;
        for (Channel channel : channels) {
            channelsNameList.append(channel.name()).append(",");
            expectedOutput.add(":john JOIN %s".formatted(channel.name()));
            expectedOutput.add(
                    ":jircd-host 353 john = %s :~bob john".formatted(channel.name()));
            expectedOutput.add(":jircd-host 366 john %s :End of /NAMES list".formatted(channel.name()));
            j += 3;
        }

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN " + channelsNameList.substring(0, channelsNameList.length() - 1));
        assertArrayEquals(expectedOutput.toArray(), connections[1].awaitMessage(j));
    }

    @Test
    void joinMultipleChannelWithPasswdTest() {
        addConnections(1);
        List<Channel> channels = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String channelName = "#" + getRandomString(5);
            connections[0].send("JOIN " + channelName);
            connections[0].ignoreMessage(4);
            Optional<Channel> channelOpt = getChannel(channelName);
            assertFalse(channelOpt.isEmpty());
            channels.add(channelOpt.get());
        }
        StringBuilder channelsNameList = new StringBuilder();
        StringBuilder passwdList       = new StringBuilder();
        List<String>  expectedOutput   = new ArrayList<>();
        int           j                = 0;
        for (Channel channel : channels) {
            channel.modes().password("thinking");
            assumeTrue(channel.modes().password().orElse("").equals("thinking"));
            channelsNameList.append(channel.name()).append(",");
            passwdList.append("thinking,");
            expectedOutput.add(":john JOIN %s".formatted(channel.name()));
            expectedOutput.add(
                    ":jircd-host 353 john = %s :~bob john".formatted(channel.name()));
            expectedOutput.add(":jircd-host 366 john %s :End of /NAMES list".formatted(channel.name()));
            j += 3;
        }

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN " + channelsNameList.substring(0, channelsNameList.length() - 1) + " " +
                            passwdList.substring(0, passwdList.length() - 1));
        assertArrayEquals(expectedOutput.toArray(), connections[1].awaitMessage(j));
    }

    @Test
    void joinMultipleChannelsWithTopicTest() {
        addConnections(1);
        List<Channel> channels = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String channelName = "#" + getRandomString(5);
            connections[0].send("JOIN " + channelName);
            connections[0].ignoreMessage(4);
            Optional<Channel> channelOpt = getChannel(channelName);
            assertFalse(channelOpt.isEmpty());
            channels.add(channelOpt.get());
        }
        StringBuilder channelsNameList = new StringBuilder();
        String[]      expectedOutput   = new String[channels.size() * 4];
        int           j                = 0;
        for (Channel channel : channels) {
            Channel.Topic exampleTopic = new Channel.Topic("This is a sample topic", server.users().get(0));
            channel.topic(exampleTopic);
            assumeTrue(channel.topic().orElse(Channel.Topic.EMPTY).equals(exampleTopic));
            channelsNameList.append(channel.name()).append(",");
            expectedOutput[j++] = ":john JOIN %s".formatted(channel.name());
            expectedOutput[j++] = ":jircd-host 332 john %s :This is a sample topic".formatted(
                    channel.name());
            expectedOutput[j++] = ":jircd-host 353 john = %s :~bob john".formatted(channel.name());
            expectedOutput[j++] = ":jircd-host 366 john %s :End of /NAMES list".formatted(channel.name());
        }

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN " + channelsNameList.substring(0, channelsNameList.length() - 1));
        assertArrayEquals(expectedOutput, connections[1].awaitMessage(expectedOutput.length));
    }

    @Test
    void joinMultipleSecretChannelsTest() {
        addConnections(1);
        List<Channel> channels = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String channelName = "#" + getRandomString(5);
            connections[0].send("JOIN " + channelName);
            connections[0].ignoreMessage(4);
            Optional<Channel> channelOpt = getChannel(channelName);
            assertFalse(channelOpt.isEmpty());
            channels.add(channelOpt.get());
        }
        StringBuilder channelsNameList = new StringBuilder();
        List<String>  expectedOutput   = new ArrayList<>();
        int           j                = 0;
        for (Channel channel : channels) {
            channel.modes().secret(true);
            assumeTrue(channel.modes().secret());
            channelsNameList.append(channel.name()).append(",");
            expectedOutput.add(":john JOIN %s".formatted(channel.name()));
            expectedOutput.add(
                    ":jircd-host 353 john @ %s :~bob john".formatted(channel.name()));
            expectedOutput.add(":jircd-host 366 john %s :End of /NAMES list".formatted(channel.name()));
            j += 3;
        }

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN " + channelsNameList.substring(0, channelsNameList.length() - 1));
        assertArrayEquals(expectedOutput.toArray(), connections[1].awaitMessage(j));
    }

    @Test
    void joinMultipleChannelWithCharacteristicsTest() {
        List<Channel> channels = new ArrayList<>();
        addConnections(1);
        for (int i = 0; i < 5; i++) {
            String channelName = "#" + getRandomString(5);
            connections[0].send("JOIN " + channelName);
            connections[0].ignoreMessage(4);
            Optional<Channel> channelOpt = getChannel(channelName);
            assertFalse(channelOpt.isEmpty());
            channels.add(channelOpt.get());
        }
        Random random = new Random();
        for (int i = 0; i < random.nextInt(channels.size()); i++) {
            channels.get(random.nextInt(channels.size())).modes().secret(true);
        }
        for (int i = 0; i < random.nextInt(channels.size()); i++) {
            channels.get(random.nextInt(channels.size())).topic(
                    new Channel.Topic("This is a sample topic", server.users().get(0)));
        }
        for (int i = 0; i < random.nextInt(channels.size()); i++) {
            channels.get(random.nextInt(channels.size())).modes().password("thinking");
        }

        StringBuilder channelsNameList = new StringBuilder();
        StringBuilder passwdList       = new StringBuilder();
        List<String>  expectedOutput   = new ArrayList<>();
        int           j                = 0;
        for (Channel channel : channels) {
            channelsNameList.append(channel.name()).append(",");
            passwdList.append(channel.modes().password().orElse("")).append(",");
            Optional<Channel.Topic> topicOpt = channel.topic();
            expectedOutput.add(":john JOIN %s".formatted(channel.name()));
            if (topicOpt.isPresent()) {
                expectedOutput.add(":jircd-host 332 john %s :%s".formatted(channel.name(),
                                                                           topicOpt.get().topic()));
                j++;
            }
            expectedOutput.add(":jircd-host 353 john %s %s :~bob john".formatted(
                    channel.modes().secret() ? "@" : "=",
                    channel.name()
            ));
            expectedOutput.add(":jircd-host 366 john %s :End of /NAMES list".formatted(channel.name()));
            j += 3;
        }

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN " + channelsNameList.substring(0, channelsNameList.length() - 1) + " " +
                            passwdList.substring(0, passwdList.length() - 1));
        assertArrayEquals(expectedOutput.toArray(), connections[1].awaitMessage(j));
    }

    @Test
    void joinInvalidChannelNameTest() {
        String channelName = "#" + getRandomString(7, 128, 255, i -> true, StandardCharsets.ISO_8859_1);
        connections[0].send("JOIN " + channelName);
        assertArrayEquals(new String[]{
                ":jircd-host 403 bob %s :No such channel".formatted(channelName)
        }, connections[0].awaitMessage());
    }

    @Test
    void joinWithManyChannelTest() {
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);

        server.supportAttribute().channelAttribute().channelLen(1);
        assumeTrue(server.supportAttribute().channelAttribute().channelLen() == 1);

        connections[0].send("JOIN #enimaloc");
        assertArrayEquals(new String[]{
                ":jircd-host 405 bob #enimaloc :You have joined too many channels"
        }, connections[0].awaitMessage());
    }

    @Test
    void joinPasswdProtectedChannelWithWrongPasswdTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        channel.modes().password("complicatedPasswd");
        assumeTrue(channel.modes().password().isPresent());

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN " + channel.name());
        assertArrayEquals(new String[]{
                ":jircd-host 475 john %s :Cannot join channel (+k)".formatted(channel
                                                                                      .name())
        }, connections[1].awaitMessage());
        assertEquals(1, channel.users().size());
    }

    @Test
    void joinBannedChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        connections[1].createUser("john", "John Doe");
        assumeFalse(server.users().isEmpty());
        channel.modes().bans().add("john!*@*");
        assumeFalse(channel.modes().bans().isEmpty());

        connections[1].send("JOIN #jircd");
        assertArrayEquals(new String[]{
                ":jircd-host 474 john #jircd :Cannot join channel (+b)"
        }, connections[1].awaitMessage());
        assertEquals(1, channel.users().size());
    }

    @Test
    void joinBannedChannelButExceptTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        connections[1].createUser("john", "John Doe");
        assumeFalse(server.users().isEmpty());
        channel.modes().bans().add("john!*@*");
        assumeFalse(channel.modes().bans().isEmpty());
        channel.modes().except().add("john!john@*");
        assumeFalse(channel.modes().except().isEmpty());

        connections[1].send("JOIN #jircd");
        assertArrayEquals(new String[]{
                ":john JOIN #jircd",
                ":jircd-host 353 john = #jircd :~bob john",
                ":jircd-host 366 john #jircd :End of /NAMES list"
        }, connections[1].awaitMessage(3));
        assertEquals(2, channel.users().size());
    }

    @Test
    void joinFullChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        channel.modes().limit(1);
        assumeTrue(channel.modes().limit().isPresent() && channel.modes().limit().getAsInt() == 1);

        baseSettings.pass().ifPresent(pass -> connections[1].send("PASS " + pass));
        connections[1].send("NICK bab");
        connections[1].send("USER babby 0 * :MAbbye Plov");
        connections[1].ignoreMessage(6 + attrLength + baseSettings.motd().length);
        connections[1].send("JOIN " + channel.name());
        assertArrayEquals(new String[]{
                ":jircd-host 471 bab %s :Cannot join channel (+l)".formatted(channel.name())
        }, connections[1].awaitMessage());
    }

    @Test
    void joinInviteOnlyChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #jircd");
        connections[0].ignoreMessage(4);
        Optional<Channel> channelOpt = getChannel("#jircd");
        assertFalse(channelOpt.isEmpty());
        Channel channel = channelOpt.get();

        channel.modes().inviteOnly(true);
        assumeTrue(channel.modes().inviteOnly());

        connections[1].createUser("john", "John Doe");
        connections[1].send("JOIN " + channel.name());
        assertArrayEquals(new String[]{
                ":jircd-host 473 john %s :Cannot join channel (+i)".formatted(channel.name())
        }, connections[1].awaitMessage());
        assertEquals(1, channel.users().size());
    }

    @Test
    void joinWith0Test() {
        assumeTrue(server.channels().isEmpty());
        connections[0].send("JOIN #a,#b,#c,#d,#e,#f,#g,#h");
        connections[0].ignoreMessage(3 * 8);
        assumeFalse(server.channels().isEmpty());

        connections[0].send("JOIN 0");
        assertArrayEquals(new String[]{
                ":bob PART #a",
                ":bob PART #b",
                ":bob PART #c",
                ":bob PART #d",
                ":bob PART #e",
                ":bob PART #f",
                ":bob PART #g",
                ":bob PART #h",
        }, connections[0].awaitMessage(8));
        assertTrue(waitFor(() -> server.channels().isEmpty()));
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
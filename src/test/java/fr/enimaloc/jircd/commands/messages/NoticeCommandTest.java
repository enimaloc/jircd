package fr.enimaloc.jircd.commands.messages;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class NoticeCommandTest extends MessageCommandBase {

    Channel channel;

    @BeforeEach
    void setUp() {
        init();
        connections[1].send("JOIN #hello");
        connections[1].ignoreMessage(3);
        assumeTrue(getChannel("#hello").isPresent());
        channel = getChannel("#hello").get();
        assumeFalse(channel.users().isEmpty());
    }

    @Test
    void noticeSameChannelTest() {
        connections[0].send("JOIN #hello");
        connections[0].ignoreMessage(3);
        connections[1].ignoreMessage();
        connections[0].send("NOTICE #hello :Hey!");
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
        assertArrayEquals(new String[]{
                ":bob NOTICE #hello :Hey!"
        }, connections[1].awaitMessage());
    }

    @Test
    void noticeNotSameChannelTest() {
        connections[0].send("NOTICE #hello :Hey!");
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
        assertArrayEquals(new String[]{
                ":bob NOTICE #hello :Hey!"
        }, connections[1].awaitMessage());
    }

    @Test
    void noticeNotSameChannelAndRefuseExternalMessageTest() {
        channel.modes().noExternalMessage(true);
        connections[0].send("NOTICE #hello :Hey!");
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
        assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
    }

    @Test
    void noticeBannedAndNoExemptTest() {
        channel.modes().bans().add("bob!*@*");
        connections[0].send("NOTICE #hello :Hey!");
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
        assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
    }

    @Test
    void noticeInModerateButNotVoiceTest() {
        connections[0].send("JOIN #hello");
        connections[0].ignoreMessage(3);
        connections[1].ignoreMessage();
        channel.modes().moderate(true);
        connections[0].send("NOTICE #hello :Hey!");
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
        assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
    }

    @Test
    void noticeInModerateWithVoiceTest() {
        connections[0].send("JOIN #hello");
        connections[0].ignoreMessage(3);
        connections[1].ignoreMessage();
        channel.modes().moderate(true);
        channel.prefix(server.users().get(0), "+");
        connections[0].send("NOTICE #hello :Hey!");
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
        assertArrayEquals(new String[]{
                ":bob NOTICE #hello :Hey!"
        }, connections[1].awaitMessage());
    }

    @Test
    void noticeToOwnerChannelTest() {
        addConnections(1);
        connections[0].send("JOIN #hello");
        connections[0].ignoreMessage(3);
        connections[1].ignoreMessage();
        connections[2].send("JOIN #hello");
        connections[2].ignoreMessage(3);
        connections[0].ignoreMessage();
        connections[1].ignoreMessage();

        connections[0].send("NOTICE ~#hello :Hey!");
        assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
        assertArrayEquals(new String[]{
                ":bob NOTICE ~#hello :Hey!"
        }, connections[1].awaitMessage());
        assertArrayEquals(EMPTY_ARRAY, connections[2].awaitMessage());
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
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
class PrivmsgCommandTest extends MessageCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }


    @Nested
    class UserToUserTest {
        @Test
        void privmsgTest() {
            connections[0].send("PRIVMSG john :Hey!");
            assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":bob PRIVMSG john :Hey!"
            }, connections[1].awaitMessage());
        }

        @Test
        void privmsgAwayUserTest() {
            server.users().get(1).away("I'm not here for now");
            connections[0].send("PRIVMSG john :Hey!");
            assertArrayEquals(new String[]{
                    ":jircd-host 301 bob john :I'm not here for now"
            }, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":bob PRIVMSG john :Hey!"
            }, connections[1].awaitMessage());
        }

        @Test
        void privmsgUnknownUserTest() {
            connections[0].send("PRIVMSG unknown :Hey!");
            assertArrayEquals(new String[]{
                    ":jircd-host 401 bob unknown :No such nick/channel"
            }, connections[0].awaitMessage());
            assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
        }
    }

    @Nested
    class UserToChannelTest {
        Channel channel;

        @BeforeEach
        void setUp() {
            connections[1].send("JOIN #hello");
            connections[1].ignoreMessage(3);
            assumeTrue(getChannel("#hello").isPresent());
            channel = getChannel("#hello").get();
            assumeFalse(channel.users().isEmpty());
        }

        @Test
        void privmsgSameChannelTest() {
            connections[0].send("JOIN #hello");
            connections[0].ignoreMessage(3);
            connections[1].ignoreMessage();
            connections[0].send("PRIVMSG #hello :Hey!");
            assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":bob PRIVMSG #hello :Hey!"
            }, connections[1].awaitMessage());
        }

        @Test
        void privmsgNotSameChannelTest() {
            connections[0].send("PRIVMSG #hello :Hey!");
            assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":bob PRIVMSG #hello :Hey!"
            }, connections[1].awaitMessage());
        }

        @Test
        void privmsgNotSameChannelAndRefuseExternalMessageTest() {
            channel.modes().noExternalMessage(true);
            connections[0].send("PRIVMSG #hello :Hey!");
            assertArrayEquals(new String[]{
                    ":jircd-host 404 bob #hello :Cannot send to channel"
            }, connections[0].awaitMessage());
            assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
        }

        @Test
        void privmsgBannedAndNoExemptTest() {
            channel.modes().bans().add("bob!*@*");
            connections[0].send("PRIVMSG #hello :Hey!");
            assertArrayEquals(new String[]{
                    ":jircd-host 404 bob #hello :Cannot send to channel"
            }, connections[0].awaitMessage());
            assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
        }

        @Test
        void privmsgInModerateButNotVoiceTest() {
            connections[0].send("JOIN #hello");
            connections[0].ignoreMessage(3);
            connections[1].ignoreMessage();
            channel.modes().moderate(true);
            connections[0].send("PRIVMSG #hello :Hey!");
            assertArrayEquals(new String[]{
                    ":jircd-host 404 bob #hello :Cannot send to channel"
            }, connections[0].awaitMessage());
            assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
        }

        @Test
        void privmsgInModerateWithVoiceTest() {
            connections[0].send("JOIN #hello");
            connections[0].ignoreMessage(3);
            connections[1].ignoreMessage();
            channel.modes().moderate(true);
            channel.prefix(server.users().get(0), "+");
            connections[0].send("PRIVMSG #hello :Hey!");
            assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":bob PRIVMSG #hello :Hey!"
            }, connections[1].awaitMessage());
        }

        @Test
        void privmsgToOwnerChannelTest() {
            addConnections(1);
            connections[2].createUser("fred", "Fred Bloggs");

            connections[0].send("JOIN #hello");
            connections[0].ignoreMessage(3);
            connections[1].ignoreMessage();
            connections[2].send("JOIN #hello");
            connections[2].ignoreMessage(3);
            connections[0].ignoreMessage();
            connections[1].ignoreMessage();

            connections[0].send("PRIVMSG ~#hello :Hey!");
            assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":bob PRIVMSG ~#hello :Hey!"
            }, connections[1].awaitMessage());
            assertArrayEquals(EMPTY_ARRAY, connections[2].awaitMessage());
        }

        @Test
        void privmsgToOperatorAndUpperTest() {
            addConnections(2);
            connections[2].createUser("fred", "Fred Bloggs");
            connections[3].createUser("tommy", "Tommy Atkins");

            connections[0].send("JOIN #hello");
            connections[0].ignoreMessage(3);
            connections[1].ignoreMessage();
            connections[2].send("JOIN #hello");
            connections[2].ignoreMessage(3);
            connections[0].ignoreMessage();
            connections[1].ignoreMessage();
            connections[3].send("JOIN #hello");
            connections[3].ignoreMessage(3);
            connections[0].ignoreMessage();
            connections[1].ignoreMessage();
            connections[2].ignoreMessage();

            Optional<User> fredOpt = channel.users()
                                            .stream()
                                            .filter(u -> u.info().username().equals("fred"))
                                            .findFirst();
            assumeTrue(fredOpt.isPresent());
            User fred = fredOpt.get();
            channel.prefix(fred, "@");

            connections[0].send("PRIVMSG @#hello :Hey!");
            assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":bob PRIVMSG @#hello :Hey!"
            }, connections[1].awaitMessage());
            assertArrayEquals(new String[]{
                    ":bob PRIVMSG @#hello :Hey!"
            }, connections[2].awaitMessage());
            assertArrayEquals(EMPTY_ARRAY, connections[3].awaitMessage());
        }
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
package fr.enimaloc.jircd.commands.server;

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
class ModeCommandTest extends ServerCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Nested
    class UserMode {

        User user;

        @BeforeEach
        void setUp() {
            assumeFalse(server.users().isEmpty());
            assumeFalse((user = server.users().get(0)) == null);
            assumeTrue(user.info().format().equals("bob"));
        }

        @Test
        void modeWithDifferentTargetTest() {
            connections[0].send("MODE notBob");
            assertArrayEquals(new String[]{
                    ":jircd-host 502 bob :Cant change mode for other users"
            }, connections[0].awaitMessage());
        }

        @Test
        void modeTest() {
            assumeTrue(user.modes().toString().isEmpty());
            connections[0].send("MODE bob");
            assertArrayEquals(new String[]{
                    ":jircd-host 221 bob "
            }, connections[0].awaitMessage());
        }

        @Nested
        class Add {
            @Test
            void modeInvisibleTest() {
                assumeFalse(user.modes().invisible());
                connections[0].send("MODE bob +i");
                assertArrayEquals(new String[]{
                        ":jircd-host 221 bob i"
                }, connections[0].awaitMessage());
                assertTrue(user.modes().invisible());
            }

            @Test
            void modeOperTest() {
                assumeFalse(user.modes().oper());
                connections[0].send("MODE bob +o");
                assertArrayEquals(new String[]{
                        ":jircd-host 221 bob "
                }, connections[0].awaitMessage());
                assertFalse(user.modes().oper());
            }

            @Test
            void modeLocalOperTest() {
                assumeFalse(user.modes().localOper());
                connections[0].send("MODE bob +O");
                assertArrayEquals(new String[]{
                        ":jircd-host 221 bob "
                }, connections[0].awaitMessage());
                assertFalse(user.modes().localOper());
            }

            @Test
            void modeWallopsTest() {
                assumeFalse(user.modes().wallops());
                connections[0].send("MODE bob +w");
                assertArrayEquals(new String[]{
                        ":jircd-host 221 bob w"
                }, connections[0].awaitMessage());
                assertTrue(user.modes().wallops());
            }

            @Test
            void modeInvalidTest() {
                connections[0].send("MODE bob +z");
                assertArrayEquals(new String[]{
                        ":jircd-host 501 bob :Unknown MODE flag"
                }, connections[0].awaitMessage());
                assertTrue(user.modes().toString().isEmpty());
            }
        }

        @Nested
        class Remove {
            @Test
            void modeInvisibleTest() {
                user.modes().invisible(true);
                assumeTrue(user.modes().invisible());

                connections[0].send("MODE bob -i");
                assertArrayEquals(new String[]{
                        ":jircd-host 221 bob "
                }, connections[0].awaitMessage());
                assertFalse(user.modes().invisible());
            }

            @Test
            void modeOperTest() {
                user.modes().oper(true);
                assumeTrue(user.modes().oper());

                connections[0].send("MODE bob -o");
                assertArrayEquals(new String[]{
                        ":jircd-host 221 bob "
                }, connections[0].awaitMessage());
                assertFalse(user.modes().oper());
            }

            @Test
            void modeLocalOperTest() {
                user.modes().localOper(true);
                assumeTrue(user.modes().localOper());

                connections[0].send("MODE bob -O");
                assertArrayEquals(new String[]{
                        ":jircd-host 221 bob "
                }, connections[0].awaitMessage());
                assertFalse(user.modes().localOper());
            }

            @Test
            void modeWallopsTest() {
                user.modes().wallops(true);
                assumeTrue(user.modes().wallops());

                connections[0].send("MODE bob -w");
                assertArrayEquals(new String[]{
                        ":jircd-host 221 bob "
                }, connections[0].awaitMessage());
                assertFalse(user.modes().wallops());
            }

            @Test
            void modeInvalidTest() {
                connections[0].send("MODE bob -z");
                assertArrayEquals(new String[]{
                        ":jircd-host 501 bob :Unknown MODE flag"
                }, connections[0].awaitMessage());
                assertTrue(user.modes().toString().isEmpty());
            }
        }
    }

    @Nested
    class ChannelMode {
        Channel channel;

        @BeforeEach
        void setUp() {
            connections[0].send("JOIN #bob");
            connections[0].ignoreMessage(3);
            Optional<Channel> channelOpt = getChannel("#bob");
            assumeTrue(waitFor(channelOpt::isPresent));
            this.channel = channelOpt.get();
        }

        @Test
        void modeWithDifferentTargetTest() {
            assumeTrue(getChannel("#unknown").isEmpty());
            connections[0].send("MODE #unknown");
            assertArrayEquals(new String[]{
                    ":jircd-host 403 bob #unknown :No such channel"
            }, connections[0].awaitMessage());
        }

        @Test
        void modeTest() {
            assumeTrue(channel.modes().modesString().isEmpty());
            connections[0].send("MODE #bob");
            assertArrayEquals(new String[]{
                    ":jircd-host 324 bob #bob  "
            }, connections[0].awaitMessage());
        }

        @Nested
        class Add {
            @Test
            void modeBanTest() {
                connections[0].send("MODE #bob +b john!*@*");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob +b john!*@*"
                }, connections[0].awaitMessage());
                assertFalse(channel.modes().bans().isEmpty());
                assertTrue(channel.modes().bans().contains("john!*@*"));
            }

            @Test
            void modeExceptsTest() {
                connections[0].send("MODE #bob +e john!john@*");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob +e john!john@*"
                }, connections[0].awaitMessage());
                assertFalse(channel.modes().except().isEmpty());
                assertTrue(channel.modes().except().contains("john!john@*"));
            }

            @Test
            void modeLimitTest() {
                connections[0].send("MODE #bob +l 5");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob +l 5"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().limit().isPresent());
                assertEquals(5, channel.modes().limit().getAsInt());
            }

            @Test
            void modeInviteOnlyTest() {
                connections[0].send("MODE #bob +i");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob +i"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().inviteOnly());
            }

            @Test
            void modeInvExTest() {
                connections[0].send("MODE #bob +I john!*@*");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob +I john!*@*"
                }, connections[0].awaitMessage());
                assertFalse(channel.modes().invEx().isEmpty());
                assertTrue(channel.modes().invEx().contains("john!*@*"));
            }

            @Test
            void modeKeyChannelTest() {
                connections[0].send("MODE #bob +k keypass");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob +k keypass"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().password().isPresent());
                assertEquals("keypass", channel.modes().password().get());
            }

            @Test
            void modeModerateTest() {
                connections[0].send("MODE #bob +m");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob +m"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().moderate());
            }

            @Test
            void modeSecretTest() {
                connections[0].send("MODE #bob +s");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob +s"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().secret());
            }

            @Test
            void modeProtectedTopicTest() {
                connections[0].send("MODE #bob +t");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob +t"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes()._protected());
            }

            @Test
            void modeNoExternalTest() {
                connections[0].send("MODE #bob +n");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob +n"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().noExternalMessage());
            }

            @Test
            void modeInvalidTest() {
                connections[0].send("MODE #bob +z");
                assertArrayEquals(new String[]{
                        ":jircd-host 501 bob :Unknown MODE flag"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().modesString().isEmpty());
            }
        }

        @Nested
        class Remove {
            @Test
            void modeBanTest() {
                channel.modes().bans().add("john!*@*");
                assumeFalse(channel.modes().bans().isEmpty());;
                assertArrayEquals(new String[]{
                        ":bob MODE #bob -b john!*@*"
                }, connections[0].send("MODE #bob -b john!*@*", 1));
                assertTrue(channel.modes().bans().isEmpty());
            }

            @Test
            void modeExceptsTest() {
                channel.modes().except().add("john!john@*");
                assumeFalse(channel.modes().except().isEmpty());
                connections[0].send("MODE #bob -e john!john@*");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob -e john!john@*"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().except().isEmpty());
            }

            @Test
            void modeLimitTest() {
                channel.modes().limit(5);
                assumeTrue(waitFor(() -> channel.modes().limit().isPresent()));
                assumeTrue(waitFor(() -> channel.modes().limit().getAsInt() == 5));
                connections[0].send("MODE #bob -l 5");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob -l 5"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().limit().isEmpty());
            }

            @Test
            void modeInviteOnlyTest() {
                channel.modes().inviteOnly(true);
                assumeTrue(channel.modes().inviteOnly());
                connections[0].send("MODE #bob -i");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob -i"
                }, connections[0].awaitMessage());
                assertFalse(channel.modes().inviteOnly());
            }

            @Test
            void modeInvExTest() {
                channel.modes().invEx().add("john!*@*");
                assumeFalse(channel.modes().invEx().isEmpty());
                connections[0].send("MODE #bob -I john!*@*");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob -I john!*@*"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().invEx().isEmpty());
            }

            @Test
            void modeKeyChannelTest() {
                channel.modes().password("keypass");
                assumeTrue(waitFor(() -> channel.modes().password().isPresent()));
                connections[0].send("MODE #bob -k keypass");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob -k *"
                }, connections[0].awaitMessage());
                assertTrue(waitFor(() -> channel.modes().password().isEmpty()));
            }

            @Test
            void modeModerateTest() {
                channel.modes().moderate(true);
                assumeTrue(channel.modes().moderate());
                connections[0].send("MODE #bob -m");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob -m"
                }, connections[0].awaitMessage());
                assertFalse(channel.modes().moderate());
            }

            @Test
            void modeSecretTest() {
                channel.modes().secret(true);
                assumeTrue(channel.modes().secret());
                connections[0].send("MODE #bob -s");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob -s"
                }, connections[0].awaitMessage());
                assertFalse(channel.modes().moderate());
            }

            @Test
            void modeProtectedTopicTest() {
                channel.modes()._protected(true);
                assumeTrue(channel.modes()._protected());
                connections[0].send("MODE #bob -t");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob -t"
                }, connections[0].awaitMessage());
                assertFalse(channel.modes()._protected());
            }

            @Test
            void modeNoExternalTest() {
                channel.modes().noExternalMessage(true);
                assumeTrue(channel.modes().noExternalMessage());
                connections[0].send("MODE #bob -n");
                assertArrayEquals(new String[]{
                        ":bob MODE #bob -n"
                }, connections[0].awaitMessage());
                assertFalse(channel.modes().noExternalMessage());
            }

            @Test
            void modeInvalidTest() {
                connections[0].send("MODE #bob -z");
                assertArrayEquals(new String[]{
                        ":jircd-host 501 bob :Unknown MODE flag"
                }, connections[0].awaitMessage());
                assertTrue(channel.modes().modesString().isEmpty());
            }
        }
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
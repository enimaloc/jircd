import com.github.enimaloc.irc.jircd.Constant;
import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.JIRCD;
import com.github.enimaloc.irc.jircd.api.ServerSettings;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.ParameterizedType;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ServerTest {
    public static final String ENDING                         = "\r\n";
    public static final long   TIME_OUT_BETWEEN_TEST          = 100;
    public static final long   TIME_OUT_BETWEEN_COMMUNICATION = 100;
    public static final int    TIME_OUT_WHEN_WAITING_RESPONSE = 1000;

    ServerSettings baseSettings;
    JIRCDImpl      server;
    Logger         logger = LoggerFactory.getLogger(ServerTest.class);

    @BeforeEach
    void setUp(TestInfo info) {
        baseSettings = new ServerSettings();

        logger.info("Creating server with settings: {}", baseSettings);
        try {
            server = (JIRCDImpl) new JIRCD.Builder()
                    .withSettings(baseSettings)
                    .build();
        } catch (IOException e) {
            fail("Can't start IRCServer", e);
        }
        logger.info("Starting {}", info.getDisplayName());
    }

    @Test
    void initialTest() {
        ServerSettings settings = server.settings();
        assertEquals(baseSettings, settings);
        assertTrue(server.users().isEmpty());
        assertTrue(server.channels().isEmpty());
    }

    @Nested
    class SocketTest {

        Connection[] connections;

        Connection createConnection() throws IOException {
            Socket client = new Socket("127.0.0.1", baseSettings.port);
            client.setSoTimeout(TIME_OUT_WHEN_WAITING_RESPONSE);
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));
            PrintStream output = new PrintStream(client.getOutputStream());
            return new Connection(client, input, output);
        }

        public void addConnections(int number) {
            Connection baseConnection = connections[0];
            this.connections    = new Connection[number + 1];
            this.connections[0] = baseConnection;
            for (int i = 0; i < number; i++) {
                try {
                    connections[i + 1] = createConnection();
                } catch (IOException e) {
                    fail("Failed to open client socket", e);
                }
            }
        }

        @BeforeEach
        void setUp(TestInfo info) {
            try {
                this.connections = new Connection[]{createConnection()};
            } catch (IOException e) {
                fail("Failed to open client socket", e);
            }
            logger.info("Starting {}", info.getDisplayName());
        }

        @Test
        @Disabled
        void fullTest() {
            connections[0].send("PASS " + baseSettings.pass);
            connections[0].send("NICK bob");
            connections[0].send("USER bobby 0 * :Mobbye Plav");
            assertArrayEquals(new String[]{
                    ":%s 001 bob :Welcome to the %s Network, bob".formatted(baseSettings.host,
                                                                            baseSettings.networkName),
                    ":%s 002 bob :Your host is %s, running version %s".formatted(baseSettings.host, Constant.NAME,
                                                                                 Constant.VERSION),
                    ":%s 003 bob :This server was created %tD %tT".formatted(baseSettings.host, server.createdAt(),
                                                                             server.createdAt()),
                    ":%s 004 bob %s %s %s %s".formatted(baseSettings.host, Constant.NAME, Constant.VERSION, "", ""),
            }, connections[0].awaitMessage(4).toArray(String[]::new));

            int              count = 0;
            SupportAttribute attr  = server.supportAttribute();
            for (int i = 0; i < Math.max(Math.ceil(attr.length() / 13.), 1); i++) {
                count++;
                List<String> messages = connections[0].awaitMessage();
                if (messages.isEmpty()) {
                    continue;
                }
                String isSupport = messages.get(0);
                assertTrue(isSupport.startsWith(":%s 005 bob ".formatted(baseSettings.host)));
                assertTrue(isSupport.endsWith(":are supported by this server"));
                isSupport = isSupport.replaceFirst(":%s 005 bob ".formatted(baseSettings.host), "")
                                     .replace(" :are supported by this server", "");

                String[] attributes = isSupport.split(" ");
                assertTrue(attributes.length <= 13);
                for (String attribute : attributes) {
                    String              key           = attribute.contains("=") ? attribute.split("=")[0] : attribute;
                    String              value         = attribute.contains("=") ? attribute.split("=")[1] : null;
                    Map<String, Object> map           = attr.asMap((s, o) -> s.equalsIgnoreCase(key));
                    String              fieldName     = (String) map.keySet().toArray()[0];
                    Object              expectedValue = map.values().toArray()[0];
                    Class<?>            expectedClazz = null;
                    Class<?>            actualClazz   = null;
                    Object              actualValue   = null;
                    if (value != null) {
                        try {
                            actualValue = Integer.parseInt(value);
                            actualClazz = Integer.class;
                        } catch (NumberFormatException ignored) {
                            if (value.equals("true") || value.equals("false")) {
                                actualValue = Boolean.parseBoolean(value);
                                actualClazz = Boolean.class;
                            } else {
                                if (value.contains(",") && Arrays.stream(value.split(",")).allMatch(
                                        s -> s.length() == 1)) {
                                    actualValue = value.toCharArray();
                                    actualClazz = Character[].class;
                                } else {
                                    actualValue = value;
                                    actualClazz = String.class;
                                }
                            }
                        }
                    }
                    if (expectedValue instanceof Optional) {
                        try {
                            if (value == null) {
                                actualClazz   = (Class<?>) ((ParameterizedType) SupportAttribute.class.getDeclaredField(
                                        fieldName).getGenericType()).getActualTypeArguments()[0];
                                expectedValue = null;
                            }
                            expectedClazz = (Class<?>) ((ParameterizedType) SupportAttribute.class.getDeclaredField(
                                    fieldName).getGenericType()).getActualTypeArguments()[0];
                            expectedValue = expectedValue != null ? ((Optional<?>) expectedValue).orElse(null) : null;
                        } catch (NoSuchFieldException e) {
                            fail(e);
                        }
                    } else if (expectedValue instanceof OptionalInt) {
                        if (value == null) {
                            actualClazz   = Integer.class;
                            expectedValue = null;
                        }
                        expectedClazz = Integer.class;
                        // Is present is not detected by idea here
                        //noinspection OptionalGetWithoutIsPresent
                        expectedValue = expectedValue != null && ((OptionalInt) expectedValue).isPresent() ?
                                ((OptionalInt) expectedValue).getAsInt() :
                                null;
                    } else {
                        expectedClazz = expectedValue.getClass();
                    }

                    assertEquals(expectedValue, actualValue);
                    assertEquals(expectedClazz, actualClazz);
                }
            }
            assertEquals(Math.max(Math.ceil(attr.length() / 13.), 1), count);
            assertEquals(1, server.users().size());
            UserImpl.Info info = server.users().get(0).info();
            assertEquals("127.0.0.1", info.host());
            assertEquals("bob", info.nickname());
            assertEquals("bobby", info.username());
            assertEquals("Mobbye Plav", info.realName());
            assertEquals("bob", info.format());
        }

        private String getRandomString(int length) {
            return getRandomString(length, i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97));
        }

        private String getRandomString(int length, IntPredicate filter) {
            return getRandomString(length, 48, 123, filter);
        }

        private String getRandomString(int length, int origin, int bound, IntPredicate filter) {
            return new Random().ints(origin, bound)
                               .filter(filter)
                               .limit(length)
                               .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                               .toString();
        }

        record Connection(Socket socket, BufferedReader input, PrintStream output) {
            public void send(String message) {
                output.print(message + ENDING);
            }

            public void ignoreMessage() {
                ignoreMessage(1);
            }

            private void ignoreMessage(int count) {
                awaitMessage(count);
            }

            public List<String> awaitMessage() {
                return awaitMessage(1);
            }

            private List<String> awaitMessage(int count) {
                List<String> messages = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    try {
                        messages.add(input.readLine());
                    } catch (SocketTimeoutException ignored) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return messages;
            }
        }

        @Nested
        class ConnectionMessage {

            @Nested
            class PassCommand {

                @Test
                void passTest() {
                    connections[0].send("PASS " + baseSettings.pass);
                    assertTrue(connections[0].awaitMessage().isEmpty());
                    UserImpl.Info info = server.users().get(0).info();
                    assertTrue(info.passwordValid());

                    assertEquals("127.0.0.1", info.host());
                    assertNull(info.username());
                    assertNull(info.nickname());
                    assertNull(info.realName());
                    assertFalse(info.canRegistrationBeComplete());
                }

                @Test
                void noParamTest() {
                    connections[0].send("PASS");
                    assertArrayEquals(new String[]{
                            ":" + baseSettings.host + " 461 @127.0.0.1 PASS :Not enough parameters"
                    }, connections[0].awaitMessage().toArray());
                    assertFalse(server.users().get(0).info().passwordValid());
                }

                @Test
                void incorrectPassTest() {
                    String passwd = getRandomString(new Random().nextInt(9) + 1);
                    assumeFalse(baseSettings.pass.equals(passwd));
                    connections[0].send("PASS " + passwd);
                    assertArrayEquals(new String[]{
                            ":" + baseSettings.host + " 464 @127.0.0.1 :Password incorrect"
                    }, connections[0].awaitMessage().toArray());
                    assertFalse(server.users().get(0).info().passwordValid());
                }

                @Test
                void alreadyRegisteredPassTest() throws InterruptedException {
                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);
                    ((UserImpl) server.users().get(0)).setState(UserState.LOGGED);
                    connections[0].send("PASS " + baseSettings.pass);
                    assertArrayEquals(new String[]{
                            ":" + baseSettings.host + " 462 @127.0.0.1 :You may not reregister"
                    }, connections[0].awaitMessage().toArray());
                }

            }

            @Nested
            class NickCommand {
                @Test
                void nickTest() throws InterruptedException {
                    connections[0].send("PASS " + baseSettings.pass);
                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);

                    connections[0].send("NICK bob");
                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);
                    UserImpl.Info info = server.users().get(0).info();
                    assertTrue(info.passwordValid());
                    assertEquals("bob", info.nickname());
                    assertArrayEquals(new String[]{}, connections[0].awaitMessage().toArray());

                    assertEquals("127.0.0.1", info.host());
                    assertNull(info.username());
                    assertNull(info.realName());
                    assertFalse(info.canRegistrationBeComplete());
                }

                @Test
                void incorrectLengthNickTest() {
                    connections[0].send("PASS " + baseSettings.pass);
                    String nickname = getRandomString(10);
                    connections[0].send("NICK " + nickname);
                    assertArrayEquals(new String[]{
                            ":" + baseSettings.host + " 432 @127.0.0.1 " + nickname + " :Erroneus nickname"
                    }, connections[0].awaitMessage().toArray());
                }

                @Test
                void incorrectNickTest() {
                    connections[0].send("PASS " + baseSettings.pass);
                    String nickname = getRandomString(7, 128, 255, i -> true);
                    connections[0].send("NICK " + nickname);
                    assertArrayEquals(new String[]{
                            ":" + baseSettings.host + " 432 @127.0.0.1 " + nickname + " :Erroneus nickname"
                    }, connections[0].awaitMessage().toArray());
                }

                @Test
                void duplicateNickTest() {
                    addConnections(1);
                    connections[0].send("PASS " + baseSettings.pass);
                    connections[1].send("PASS " + baseSettings.pass);
                    connections[0].send("NICK dup");
                    connections[1].ignoreMessage();
                    connections[1].send("NICK dup");
                    assertArrayEquals(new String[]{
                            ":" + baseSettings.host + " 433 @127.0.0.1 dup :Nickname is already in use"
                    }, connections[1].awaitMessage().toArray());
                }
            }

            @Nested
            class UserCommand {
                @Test
                void userTest() throws InterruptedException {
                    connections[0].send("PASS " + baseSettings.pass);
                    connections[0].send("USER bobby 0 * :Mobbye Plav" + ENDING);
                    Thread.sleep(100);
                    UserImpl.Info info = server.users().get(0).info();
                    assertEquals("bobby", info.username());
                    assertEquals("Mobbye Plav", info.realName());
                    assertTrue(info.passwordValid());

                    assertEquals("127.0.0.1", info.host());
                    assertNull(info.nickname());
                    assertFalse(info.canRegistrationBeComplete());
                }

                @Test
                void alreadyRegisteredUserTest() {
                    connections[0].send("PASS " + baseSettings.pass);
                    connections[0].send("NICK bob");
                    connections[0].send("USER bobby 0 * :Mobby Plav");
                    connections[0].ignoreMessage(
                            (int) (5 + Math.max(Math.ceil(server.supportAttribute().length() / 13.), 1)));
                    connections[0].send("USER bob 0 * :Mobba Plav");
                    assertArrayEquals(new String[]{
                            ":" + baseSettings.host + " 462 bob :You may not reregister"
                    }, connections[0].awaitMessage().toArray());
                }
            }

            @Nested
            class OperCommand {
                @Test
                void operTest() {
                    ServerSettings.Operator savedOper = baseSettings.operators.get(0);
                    connections[0].send("OPER " + savedOper.username() + " " + savedOper.password());
                    assertArrayEquals(new String[]{
                            ":%s 381 @127.0.0.1 :You are now an IRC operator".formatted(baseSettings.host)
                    }, connections[0].awaitMessage().toArray(new String[0]));
                }

                @Test
                void incorrectPasswdOperTest() {
                    ServerSettings.Operator savedOper = baseSettings.operators.get(0);
                    connections[0].send(
                            "OPER " + savedOper.username() + " " + getRandomString(new Random().nextInt(9) + 1));
                    assertArrayEquals(new String[]{
                            ":%s 464 @127.0.0.1 :Password incorrect".formatted(baseSettings.host)
                    }, connections[0].awaitMessage().toArray(new String[0]));
                }

                @Test
                void incorrectParamsNumberOperTest() {
                    connections[0].send("OPER");
                    assertArrayEquals(new String[]{
                            ":%s 461 @127.0.0.1 OPER :Not enough parameters".formatted(baseSettings.host)
                    }, connections[0].awaitMessage().toArray(new String[0]));
                }
            }

            @Nested
            class QuitCommand {
                @Test
                void quitTest() throws InterruptedException {
                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);
                    assertEquals(1, server.users().size());
                    User user = server.users().get(0);
                    connections[0].send("QUIT");

                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);
                    assertTrue(server.users().isEmpty());
                    assertEquals(UserState.DISCONNECTED, user.state());
                }

                @Test
                void quitWithReasonTest() throws InterruptedException {
                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);
                    assertEquals(1, server.users().size());
                    User user = server.users().get(0);
                    connections[0].send("QUIT :Bye");

                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);
                    assertTrue(server.users().isEmpty());
                    assertEquals(UserState.DISCONNECTED, user.state());
                }
            }

        }
    }

    @AfterEach
    void tearDown(TestInfo info) throws InterruptedException {
        logger.info("{} test end", info.getDisplayName());
        server.shutdown();
        Thread.sleep(TIME_OUT_BETWEEN_TEST); // Await server shutdown
    }
}
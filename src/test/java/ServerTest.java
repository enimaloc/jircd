import com.github.enimaloc.irc.jircd.Constant;
import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.JIRCD;
import com.github.enimaloc.irc.jircd.api.ServerSettings;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.*;
import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.net.BindException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.FullModuleTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ServerTest {
    public static final String ENDING                         = "\r\n";
    public static final long   TIME_OUT_BETWEEN_COMMUNICATION = 100;
    public static final int    TIME_OUT_WHEN_WAITING_RESPONSE = 1000 * 5;

    public static final String[] EMPTY_ARRAY = new String[0];

    static ServerSettings baseSettings;
    static int            attrLength;
    JIRCDImpl server;
    Logger    logger = LoggerFactory.getLogger(ServerTest.class);

    @BeforeEach
    void setUp(TestInfo info) {
        baseSettings = new ServerSettings();

        baseSettings.motd = new String[0];

        logger.info("Creating server with settings: {}", baseSettings);
        boolean retry = true;
        while (retry && server == null) {
            try {
                server     = (JIRCDImpl) new JIRCD.Builder()
                        .withSettings(baseSettings)
                        .build();
                attrLength = (int) Math.max(Math.ceil(server.supportAttribute().length() / 13.), 1);
                retry      = false;
            } catch (BindException ignored) {
                int newPort = new Random().nextInt(1000) + 1024;
                logger.warn("Port {} is currently used, replaced with {}", baseSettings.port, newPort);
                baseSettings.port = newPort;
            } catch (IOException e) {
                retry = false;
                fail("Can't start IRCServer", e);
            }
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

    void setSettings(ServerSettings settings) {
        Arrays.stream(ServerSettings.class.getDeclaredFields())
              .forEach(field -> {
                  field.setAccessible(true);
                  try {
                      field.set(server.settings(), field.get(settings));
                  } catch (IllegalAccessException e) {
                      e.printStackTrace();
                  }
              });
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
            Connection[] clone = connections.clone();
            this.connections = new Connection[clone.length + number];
            System.arraycopy(clone, 0, connections, 0, clone.length);
            for (int i = 0; i < number; i++) {
                try {
                    connections[i + clone.length] = createConnection();
                } catch (IOException e) {
                    fail("Failed to open client socket", e);
                }
            }
        }

        public boolean waitFor(int timeout, TimeUnit unit) {
            return !waitFor(() -> false, timeout, unit);
        }

        public boolean waitFor(BooleanSupplier condition) {
            return waitFor(condition, true);
        }

        public boolean waitFor(BooleanSupplier condition, boolean expected) {
            return waitFor(condition, expected, 5, TimeUnit.SECONDS);
        }

        public boolean waitFor(BooleanSupplier condition, int timeout, TimeUnit unit) {
            return waitFor(condition, true, timeout, unit);
        }

        public boolean waitFor(BooleanSupplier condition, boolean expected, int timeout, TimeUnit unit) {
            long timedOut = System.currentTimeMillis() + unit.toMillis(timeout);
            while (condition.getAsBoolean() != expected) {
                if (System.currentTimeMillis() >= timedOut) {
                    return false;
                }
            }
            return true;
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

        @FullModuleTest
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
            public void createUser(String user, String realName) {
                createUser(user, user, realName);
            }

            public void createUser(String nick, String user, String realName) {
                send("PASS %s".formatted(baseSettings.pass));
                send("NICK %s".formatted(nick));
                send("USER %s 0 * :%s".formatted(user, realName));
                ignoreMessage(4 + attrLength + Math.max(1, baseSettings.motd.length));
            }

            public void send(String message) {
                output.print(message + ENDING);
            }

            public void ignoreMessage() {
                ignoreMessage(1);
            }

            public void ignoreMessage(int count) {
                awaitMessage(count);
            }

            public List<String> awaitMessage() {
                return awaitMessage(1);
            }

            public List<String> awaitMessage(int count) {
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

            @FullModuleTest
            void fullConnectionTest() {
                String host = baseSettings.host;
                connections[0].send("PASS " + baseSettings.pass);
                assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage().toArray());
                connections[0].send("NICK bob");
                assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage().toArray());
                connections[0].send("USER bobby 0 * :Mobbye Plav");

                assertArrayEquals(new String[]{
                        ":%s 001 bob :Welcome to the %s Network, bob".formatted(host, baseSettings.networkName),
                        ":%s 002 bob :Your host is %s, running version %s".formatted(host, Constant.NAME,
                                                                                     Constant.VERSION),
                        ":%s 003 bob :This server was created %tD %tT".formatted(host, server.createdAt(),
                                                                                 server.createdAt()),
                        ":%s 004 bob %s %s %s %s".formatted(host, Constant.NAME, Constant.VERSION, "", ""),
                }, connections[0].awaitMessage(4).toArray());

                SupportAttribute attr = server.supportAttribute();
                for (int i = 0; i < attrLength; i++) {
                    List<String> messages = connections[0].awaitMessage();
                    if (messages.isEmpty()) {
                        continue;
                    }
                    String isSupport = messages.get(0);
                    System.out.println(isSupport);
                    assertTrue(isSupport.startsWith(":%s 005 bob ".formatted(baseSettings.host)));
                    assertTrue(isSupport.endsWith(":are supported by this server"));
                    isSupport = isSupport.replaceFirst(":%s 005 bob ".formatted(baseSettings.host), "")
                                         .replace(" :are supported by this server", "");

                    String[] attributes = isSupport.split(" ");
                    assertTrue(attributes.length <= 13);
                    for (String attribute : attributes) {
                        String key = attribute.contains("=") ?
                                attribute.split("=")[0] :
                                attribute;
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
                                    actualClazz
                                                  = (Class<?>) ((ParameterizedType) SupportAttribute.class.getDeclaredField(
                                            fieldName).getGenericType()).getActualTypeArguments()[0];
                                    expectedValue = null;
                                }
                                expectedClazz = (Class<?>) ((ParameterizedType) SupportAttribute.class.getDeclaredField(
                                        fieldName).getGenericType()).getActualTypeArguments()[0];
                                expectedValue = expectedValue != null ?
                                        ((Optional<?>) expectedValue).orElse(null) :
                                        null;
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

                assertEquals(":%s 422 bob :MOTD File is missing".formatted(host),
                             connections[0].awaitMessage().get(0));
                User bob = server.users().get(0);
                assertEquals(UserState.LOGGED, bob.state());

                connections[0].send("QUIT");
                assertTrue(waitFor(() -> bob.state() == UserState.DISCONNECTED, 1, TimeUnit.SECONDS));
            }

            @FullModuleTest
            void fullConnectionWithMOTDTest() throws IOException {
                String host     = baseSettings.host;
                File   tempFile = File.createTempFile("motd", "txt");
                tempFile.deleteOnExit();
                System.out.println("tempFile.getAbsolutePath() = " + tempFile.getAbsolutePath());
                FileWriter writer = new FileWriter(tempFile);
                writer.write("Custom motd set in " + tempFile.getAbsolutePath());
                writer.close();

                setSettings(baseSettings.copy(new ServerSettings(tempFile), field -> !field.getName().equals("motd")));
                System.out.println("server.settings() = " + server.settings());

                connections[0].send("PASS " + baseSettings.pass);
                assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage().toArray());

                connections[0].send("NICK bob");
                assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage().toArray());

                connections[0].send("USER bobby 0 * :Mobbye Plav");
                assertArrayEquals(new String[]{
                        ":%s 001 bob :Welcome to the %s Network, bob".formatted(host, baseSettings.networkName),
                        ":%s 002 bob :Your host is %s, running version %s".formatted(host, Constant.NAME,
                                                                                     Constant.VERSION),
                        ":%s 003 bob :This server was created %tD %tT".formatted(host, server.createdAt(),
                                                                                 server.createdAt()),
                        ":%s 004 bob %s %s %s %s".formatted(host, Constant.NAME, Constant.VERSION, "", ""),
                }, connections[0].awaitMessage(4).toArray());

                SupportAttribute attr = server.supportAttribute();
                for (int i = 0; i < attrLength; i++) {
                    List<String> messages = connections[0].awaitMessage();
                    if (messages.isEmpty()) {
                        continue;
                    }
                    String isSupport = messages.get(0);
                    System.out.println(isSupport);
                    assertTrue(isSupport.startsWith(":%s 005 bob ".formatted(baseSettings.host)));
                    assertTrue(isSupport.endsWith(":are supported by this server"));
                    isSupport = isSupport.replaceFirst(":%s 005 bob ".formatted(baseSettings.host), "")
                                         .replace(" :are supported by this server", "");

                    String[] attributes = isSupport.split(" ");
                    assertTrue(attributes.length <= 13);
                    for (String attribute : attributes) {
                        String key = attribute.contains("=") ?
                                attribute.split("=")[0] :
                                attribute;
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
                                    actualClazz
                                                  = (Class<?>) ((ParameterizedType) SupportAttribute.class.getDeclaredField(
                                            fieldName).getGenericType()).getActualTypeArguments()[0];
                                    expectedValue = null;
                                }
                                expectedClazz = (Class<?>) ((ParameterizedType) SupportAttribute.class.getDeclaredField(
                                        fieldName).getGenericType()).getActualTypeArguments()[0];
                                expectedValue = expectedValue != null ?
                                        ((Optional<?>) expectedValue).orElse(null) :
                                        null;
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

                assertEquals(":%s 375 bob :- %s Message of the day - ".formatted(host, host),
                             connections[0].awaitMessage().get(0));
                for (String motd : baseSettings.motd) {
                    assertEquals(":%s 372 bob :%s".formatted(host, motd), connections[0].awaitMessage().get(0));
                }
                assertEquals(":%s 376 bob :End of /MOTD command.".formatted(host),
                             connections[0].awaitMessage().get(0));
                User bob = server.users().get(0);
                assertEquals(UserState.LOGGED, bob.state());

                connections[0].send("QUIT");
                assertTrue(waitFor(() -> bob.state() == UserState.DISCONNECTED, 1, TimeUnit.SECONDS));
            }

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
                    String nickname = getRandomString(50);
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
                    assertTrue(waitFor(500, TimeUnit.MILLISECONDS));
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
                    connections[0].ignoreMessage(6 + attrLength + baseSettings.motd.length);
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
                    }, connections[0].awaitMessage().toArray());
                }

                @Test
                void incorrectPasswdOperTest() {
                    ServerSettings.Operator savedOper = baseSettings.operators.get(0);
                    connections[0].send(
                            "OPER " + savedOper.username() + " " + getRandomString(new Random().nextInt(9) + 1));
                    assertArrayEquals(new String[]{
                            ":%s 464 @127.0.0.1 :Password incorrect".formatted(baseSettings.host)
                    }, connections[0].awaitMessage().toArray(EMPTY_ARRAY));
                }

                @Test
                void incorrectParamsNumberOperTest() {
                    connections[0].send("OPER");
                    assertArrayEquals(new String[]{
                            ":%s 461 @127.0.0.1 OPER :Not enough parameters".formatted(baseSettings.host)
                    }, connections[0].awaitMessage().toArray());
                }
            }

            @Nested
            class QuitCommand {
                @Test
                void quitTest() throws InterruptedException {
                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);
                    assumeTrue(waitFor(() -> server.users().size() == 1));
                    User user = server.users().get(0);
                    connections[0].send("QUIT");

                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);
                    assertTrue(server.users().isEmpty());
                    assertEquals(UserState.DISCONNECTED, user.state());
                }

                @Test
                void quitWithReasonTest() throws InterruptedException {
                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);
                    assumeTrue(waitFor(() -> server.users().size() == 1));
                    User user = server.users().get(0);
                    connections[0].send("QUIT :Bye");

                    Thread.sleep(TIME_OUT_BETWEEN_COMMUNICATION);
                    assertTrue(server.users().isEmpty());
                    assertEquals(UserState.DISCONNECTED, user.state());
                }
            }
        }

        @Nested
        class ChannelOperation {

            Optional<Channel> getChannel(String name) {
                return server.channels().stream().filter(c -> c.name().equals(name)).findFirst();
            }

            @BeforeEach
            void setUp() {
                connections[0].createUser("bob", "bobby", "Mobbye Plav");
            }

            @FullModuleTest
            void fullChannelOperationTest() {
                assumeTrue(server.channels().isEmpty());
                connections[0].send("LIST");
                assertArrayEquals(new String[]{
                        ":%s 321 bob Channel :Users  Name".formatted(baseSettings.host),
                        ":%s 323 bob :End of /LIST".formatted(baseSettings.host)
                }, connections[0].awaitMessage(2).toArray());

                connections[0].send("JOIN #jircd");
                assertArrayEquals(new String[]{
                        ":bob JOIN #jircd",
                        ":%s 353 bob = #jircd :%%bob".formatted(baseSettings.host),
                        ":%s 366 bob #jircd :End of /NAMES list".formatted(baseSettings.host)
                }, connections[0].awaitMessage(3).toArray());
                assertFalse(server.channels().isEmpty());
                Optional<Channel> jircdOpt = getChannel("#jircd");
                assertFalse(jircdOpt.isEmpty());
                Channel jircd = jircdOpt.get();
                assertFalse(jircd.users().isEmpty());

                assertTrue(jircd.topic().isEmpty());
                connections[0].send("TOPIC #jircd :A java internet relay chat deamon");
                assertArrayEquals(new String[]{
                        ":%s 332 bob #jircd :A java internet relay chat deamon".formatted(baseSettings.host)
                }, connections[0].awaitMessage().toArray());
                assertTrue(jircd.topic().isPresent());

                connections[0].send("NAMES #jircd");
                assertArrayEquals(new String[]{
                        ":%s 353 bob = #jircd :%%bob".formatted(baseSettings.host),
                        ":%s 366 bob #jircd :End of /NAMES list".formatted(baseSettings.host)
                }, connections[0].awaitMessage(2).toArray());

                connections[0].send("LIST");
                assertArrayEquals(new String[]{
                        ":%s 321 bob Channel :Users  Name".formatted(baseSettings.host),
                        ":%s 322 bob #jircd 1 :A java internet relay chat deamon".formatted(baseSettings.host),
                        ":%s 323 bob :End of /LIST".formatted(baseSettings.host)
                }, connections[0].awaitMessage(3).toArray());

                connections[0].send("PART #jircd");
                assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage().toArray());
                assertTrue(getChannel("#jircd").isEmpty());
            }

            @Nested
            class JoinCommand {

                @Test
                void joinChannelTest() {
                    connections[0].send("JOIN #jircd");

                    assertArrayEquals(new String[]{
                            ":bob JOIN #jircd",
                            ":%s 353 bob = #jircd :%%bob".formatted(baseSettings.host),
                            ":%s 366 bob #jircd :End of /NAMES list".formatted(baseSettings.host)
                    }, connections[0].awaitMessage(3).toArray());

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
                            ":%s 332 john #jircd :Example topic".formatted(baseSettings.host),
                            ":%s 353 john = #jircd :%%bob john".formatted(baseSettings.host),
                            ":%s 366 john #jircd :End of /NAMES list".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(4).toArray());
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
                            ":%s 353 john @ #jircd :%%bob john".formatted(baseSettings.host),
                            ":%s 366 john #jircd :End of /NAMES list".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(4).toArray());
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
                            ":%s 353 john = #jircd :%%bob john".formatted(baseSettings.host),
                            ":%s 366 john #jircd :End of /NAMES list".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(4).toArray());
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
                                ":%s 353 john = %s :%%bob john".formatted(baseSettings.host, channel.name()));
                        expectedOutput.add(":%s 366 john %s :End of /NAMES list".formatted(baseSettings.host,
                                                                                           channel.name()));
                        j += 3;
                    }

                    connections[1].createUser("john", "John Doe");
                    connections[1].send("JOIN " + channelsNameList.substring(0, channelsNameList.length() - 1));
                    assertArrayEquals(expectedOutput.toArray(), connections[1].awaitMessage(j).toArray());
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
                                ":%s 353 john = %s :%%bob john".formatted(baseSettings.host, channel.name()));
                        expectedOutput.add(":%s 366 john %s :End of /NAMES list".formatted(baseSettings.host,
                                                                                           channel.name()));
                        j += 3;
                    }

                    connections[1].createUser("john", "John Doe");
                    connections[1].send("JOIN " + channelsNameList.substring(0, channelsNameList.length() - 1) + " " +
                                        passwdList.substring(0, passwdList.length() - 1));
                    assertArrayEquals(expectedOutput.toArray(), connections[1].awaitMessage(j).toArray());
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
                        expectedOutput[j++] = ":%s 332 john %s :This is a sample topic".formatted(baseSettings.host,
                                                                                                  channel.name());
                        expectedOutput[j++] = ":%s 353 john = %s :%%bob john".formatted(baseSettings.host,
                                                                                        channel.name());
                        expectedOutput[j++] = ":%s 366 john %s :End of /NAMES list".formatted(baseSettings.host,
                                                                                              channel.name());
                    }

                    connections[1].createUser("john", "John Doe");
                    connections[1].send("JOIN " + channelsNameList.substring(0, channelsNameList.length() - 1));
                    assertArrayEquals(expectedOutput, connections[1].awaitMessage(expectedOutput.length).toArray());
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
                                ":%s 353 john @ %s :%%bob john".formatted(baseSettings.host, channel.name()));
                        expectedOutput.add(":%s 366 john %s :End of /NAMES list".formatted(baseSettings.host,
                                                                                           channel.name()));
                        j += 3;
                    }

                    connections[1].createUser("john", "John Doe");
                    connections[1].send("JOIN " + channelsNameList.substring(0, channelsNameList.length() - 1));
                    assertArrayEquals(expectedOutput.toArray(), connections[1].awaitMessage(j).toArray());
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
                            expectedOutput.add(":%s 332 john %s :%s".formatted(baseSettings.host,
                                                                               channel.name(),
                                                                               topicOpt.get().topic()));
                            j++;
                        }
                        expectedOutput.add(":%s 353 john %s %s :%%bob john".formatted(
                                baseSettings.host,
                                channel.modes().secret() ? "@" : "=",
                                channel.name()
                        ));
                        expectedOutput.add(":%s 366 john %s :End of /NAMES list".formatted(baseSettings.host,
                                                                                           channel.name()));
                        j += 3;
                    }

                    connections[1].createUser("john", "John Doe");
                    connections[1].send("JOIN " + channelsNameList.substring(0, channelsNameList.length() - 1) + " " +
                                        passwdList.substring(0, passwdList.length() - 1));
                    assertArrayEquals(expectedOutput.toArray(), connections[1].awaitMessage(j).toArray());
                }

                @Test
                void joinInvalidChannelNameTest() {
                    String channelName = "#" + getRandomString(7, 128, 255, i -> true);
                    connections[0].send("JOIN " + channelName);
                    assertArrayEquals(new String[]{
                            ":%s 403 bob %s :No such channel".formatted(baseSettings.host, channelName)
                    }, connections[0].awaitMessage().toArray());
                }

                @Test
                void joinWithManyChannelTest() {
                    connections[0].send("JOIN #jircd");
                    connections[0].ignoreMessage(4);

                    server.supportAttribute().channelLen(1);
                    assumeTrue(server.supportAttribute().channelLen() == 1);

                    connections[0].send("JOIN #enimaloc");
                    assertArrayEquals(new String[]{
                            ":%s 405 bob #enimaloc :You have joined too many channels".formatted(baseSettings.host)
                    }, connections[0].awaitMessage().toArray());
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
                            ":%s 475 john %s :Cannot join channel (+k)".formatted(baseSettings.host,
                                                                                  channel
                                                                                          .name())
                    }, connections[1].awaitMessage().toArray());
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
                    ((ChannelImpl) channel).modifiableBans().add(server.users().get(1));
                    assumeFalse(channel.bans().isEmpty());

                    connections[1].send("JOIN #jircd");
                    assertArrayEquals(new String[]{
                            ":%s 474 john #jircd :Cannot join channel (+b)".formatted(baseSettings.host)
                    }, connections[1].awaitMessage().toArray());
                    assertEquals(1, channel.users().size());
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

                    connections[1].send("PASS " + baseSettings.pass);
                    connections[1].send("NICK bab");
                    connections[1].send("USER babby 0 * :MAbbye Plov");
                    connections[1].ignoreMessage(6 + attrLength + baseSettings.motd.length);
                    connections[1].send("JOIN " + channel.name());
                    assertArrayEquals(new String[]{
                            ":%s 471 bab %s :Cannot join channel (+l)".formatted(baseSettings.host, channel.name())
                    }, connections[1].awaitMessage().toArray());
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
                            ":%s 473 john %s :Cannot join channel (+i)".formatted(baseSettings.host,
                                                                                  channel.name())
                    }, connections[1].awaitMessage().toArray());
                    assertEquals(1, channel.users().size());
                }

                @Test
                void joinWith0Test() {
                    assumeTrue(server.channels().isEmpty());
                    connections[0].send("JOIN #a,#b,#c,#d,#e,#f,#g,#h");
                    connections[0].ignoreMessage(3 * 8);
                    assumeFalse(server.channels().isEmpty());

                    connections[0].send("JOIN 0");
                    assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage().toArray());
                    assertTrue(server.channels().isEmpty());
                }
            }

            @Nested
            class PartCommand {
                @Test
                void partTest() {
                    connections[0].send("JOIN #jircd");
                    connections[0].ignoreMessage(4);
                    Optional<Channel> channelOpt = getChannel("#jircd");
                    assertFalse(channelOpt.isEmpty());
                    Channel channel = channelOpt.get();

                    assertFalse(server.users().get(0).channels().isEmpty());
                    connections[0].send("PART " + channel.name());
                    assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage().toArray());
                }

                @Test
                void partWithReason() {
                    connections[0].send("JOIN #jircd");
                    connections[0].ignoreMessage(4);
                    Optional<Channel> channelOpt = getChannel("#jircd");
                    assertFalse(channelOpt.isEmpty());
                    Channel channel = channelOpt.get();

                    assertFalse(server.users().get(0).channels().isEmpty());
                    connections[0].send("PART " + channel.name() + ": Bye!");
                    assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage().toArray());
                }

                @Test
                void ppartMultipleTest() {
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
                    assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage(channels.size()).toArray());
                    assertTrue(server.users().get(1).channels().isEmpty());
                }

                @Test
                void partWithoutParametersTest() {
                    connections[0].send("PART");
                    assertArrayEquals(new String[]{
                            ":%s 461 bob PART :Not enough parameters".formatted(baseSettings.host)
                    }, connections[0].awaitMessage().toArray());
                }

                @Test
                void partInvalidChannelNameTest() {
                    String channelName = getRandomString(7, 128, 255, i -> true);
                    connections[0].send("PART " + channelName);
                    assertArrayEquals(new String[]{
                            ":%s 403 bob %s :No such channel".formatted(baseSettings.host, channelName)
                    }, connections[0].awaitMessage().toArray());
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
                            ":%s 442 john #jircd :You're not on that channel".formatted(baseSettings.host)
                    }, connections[1].awaitMessage().toArray());
                }
            }

            @Nested
            class TopicCommand {
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
                            ":%s 331 bob #jircd :No topic is set".formatted(baseSettings.host)
                    }, connections[0].awaitMessage().toArray());
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
                            ":%s 332 john #jircd :Example topic".formatted(baseSettings.host),
                            ":%s 333 john #jircd bob %s".formatted(baseSettings.host, topic.unixTimestamp())
                    }, connections[1].awaitMessage(2).toArray());
                }

                @Test
                void topicWithNoParameterTest() {
                    connections[0].send("TOPIC");
                    assertArrayEquals(new String[]{
                            ":%s 461 bob TOPIC :Not enough parameters".formatted(baseSettings.host)
                    }, connections[0].awaitMessage().toArray());
                }

                @Test
                void topicIncorrectChannelNameTest() {
                    String channelName = "#" + getRandomString(7, 128, 255, i -> true);
                    connections[0].send("TOPIC " + channelName);
                    assertArrayEquals(new String[]{
                            ":%s 403 bob %s :No such channel".formatted(baseSettings.host, channelName)
                    }, connections[0].awaitMessage().toArray());
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
                            ":%s 442 john #jircd :You're not on that channel".formatted(baseSettings.host)
                    }, connections[1].awaitMessage().toArray());
                }

                @Test
                void topicChangeButNoPermTest() {
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
                            ":%s 482 john #jircd :You're not channel operator".formatted(baseSettings.host)
                    }, connections[1].awaitMessage().toArray());
                    assertTrue(channel.topic().isEmpty());
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
                            ":%s 332 bob #jircd :Another topic".formatted(baseSettings.host)
                    }, connections[0].awaitMessage().toArray());
                    assertFalse(channel.topic().isEmpty());
                }
            }

            @Nested
            class NamesCommand {
                @Test
                void namesTest() {
                    addConnections(1);
                    connections[0].send("JOIN #names");
                    connections[0].ignoreMessage(4);

                    connections[1].createUser("john", "John Doe");
                    connections[1].send("NAMES #names");
                    assertArrayEquals(new String[]{
                            ":%s 353 john = #names :%%bob".formatted(baseSettings.host),
                            ":%s 366 john #names :End of /NAMES list".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(2).toArray());
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
                            ":%s 353 fred = #names :%%bob john".formatted(baseSettings.host),
                            ":%s 366 fred #names :End of /NAMES list".formatted(baseSettings.host),
                            ":%s 353 fred = #joker :%%john".formatted(baseSettings.host),
                            ":%s 366 fred #joker :End of /NAMES list".formatted(baseSettings.host)
                    }, connections[2].awaitMessage(4).toArray());
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
                            ":%s 366 john #names :End of /NAMES list".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(1).toArray());
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
                            ":%s 353 john @ #names :%%bob john".formatted(baseSettings.host),
                            ":%s 366 john #names :End of /NAMES list".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(2).toArray());
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
                            ":%s 353 john = #names :".formatted(baseSettings.host),
                            ":%s 366 john #names :End of /NAMES list".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(2).toArray());
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
                            ":%s 353 john = #names :%%bob john".formatted(baseSettings.host),
                            ":%s 366 john #names :End of /NAMES list".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(2).toArray());
                }
            }

            @Nested
            class ListCommand {
                @BeforeEach
                void setUp() {
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
                            ":%s 321 john Channel :Users  Name".formatted(baseSettings.host),
                            ":%s 322 john #hello 1 :".formatted(baseSettings.host),
                            ":%s 322 john #bonjour 1 :".formatted(baseSettings.host),
                            ":%s 322 john #hey 1 :".formatted(baseSettings.host),
                            ":%s 322 john #bye 1 :".formatted(baseSettings.host),
                            ":%s 323 john :End of /LIST".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(6).toArray());
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
                            ":%s 321 ann Channel :Users  Name".formatted(baseSettings.host),
                            ":%s 322 ann #hello 3 :".formatted(baseSettings.host),
                            ":%s 322 ann #bonjour 4 :".formatted(baseSettings.host),
                            ":%s 322 ann #bye 3 :".formatted(baseSettings.host),
                            ":%s 323 ann :End of /LIST".formatted(baseSettings.host)
                    }, connections[4].awaitMessage(5).toArray());
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
                            ":%s 321 ann Channel :Users  Name".formatted(baseSettings.host),
                            ":%s 322 ann #hey 1 :".formatted(baseSettings.host),
                            ":%s 323 ann :End of /LIST".formatted(baseSettings.host)
                    }, connections[4].awaitMessage(3).toArray());
                }

                @Test
                void listWithMask() {
                    connections[0].send("JOIN #42,#azerty,#AZERTY,#1abc2");
                    connections[0].ignoreMessage(3 * 4);

                    connections[1].send("LIST #[0-9]*");
                    assertArrayEquals(new String[]{
                            ":%s 321 john Channel :Users  Name".formatted(baseSettings.host),
                            ":%s 322 john #42 1 :".formatted(baseSettings.host),
                            ":%s 323 john :End of /LIST".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(3).toArray());

                    connections[1].send("LIST #[0-9][abc]{3}[0-9]");
                    assertArrayEquals(new String[]{
                            ":%s 321 john Channel :Users  Name".formatted(baseSettings.host),
                            ":%s 322 john #1abc2 1 :".formatted(baseSettings.host),
                            ":%s 323 john :End of /LIST".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(3).toArray());

                    connections[1].send("LIST #[a-z]*");
                    assertArrayEquals(new String[]{
                            ":%s 321 john Channel :Users  Name".formatted(baseSettings.host),
                            ":%s 322 john #azerty 1 :".formatted(baseSettings.host),
                            ":%s 323 john :End of /LIST".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(3).toArray());

                    connections[1].send("LIST #[A-Z]*");
                    assertArrayEquals(new String[]{
                            ":%s 321 john Channel :Users  Name".formatted(baseSettings.host),
                            ":%s 322 john #AZERTY 1 :".formatted(baseSettings.host),
                            ":%s 323 john :End of /LIST".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(3).toArray());

                    connections[1].send("LIST #.*");
                    assertArrayEquals(new String[]{
                            ":%s 321 john Channel :Users  Name".formatted(baseSettings.host),
                            ":%s 322 john #42 1 :".formatted(baseSettings.host),
                            ":%s 322 john #azerty 1 :".formatted(baseSettings.host),
                            ":%s 322 john #AZERTY 1 :".formatted(baseSettings.host),
                            ":%s 322 john #1abc2 1 :".formatted(baseSettings.host),
                            ":%s 323 john :End of /LIST".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(6).toArray());

                    connections[1].send("LIST #");
                    assertArrayEquals(new String[]{
                            ":%s 321 john Channel :Users  Name".formatted(baseSettings.host),
                            ":%s 323 john :End of /LIST".formatted(baseSettings.host)
                    }, connections[1].awaitMessage(2).toArray());
                }
            }
        }
    }

    @AfterEach
    void tearDown(TestInfo info) throws InterruptedException {
        logger.info("{} test end", info.getDisplayName());
        server.shutdown();
    }
}
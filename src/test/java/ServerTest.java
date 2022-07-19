import fr.enimaloc.jircd.*;
import fr.enimaloc.jircd.server.JIRCD;
import fr.enimaloc.jircd.server.ServerSettings;
import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.server.attributes.SupportAttribute;
import fr.enimaloc.jircd.user.User;
import fr.enimaloc.jircd.user.UserInfo;
import fr.enimaloc.jircd.user.UserState;
import fr.enimaloc.enutils.classes.NumberUtils;
import java.io.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.net.BindException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.FullModuleTest;
import utils.ListUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ServerTest {
    public static final String ENDING                        = "\r\n";
    public static final long   TIMEOUT_BETWEEN_COMMUNICATION = NumberUtils.getSafe(
            System.getenv("TIMEOUT_BETWEEN_COMMUNICATION"), Long.class).orElse(1000L);
    public static final int    TIMEOUT_WHEN_WAITING_RESPONSE = NumberUtils.getSafe(
            System.getenv("TIMEOUT_WHEN_WAITING_RESPONSE"), Integer.class).orElse(1000);

    public static final String[] EMPTY_ARRAY  = new String[0];
    public static final String[] SOCKET_CLOSE = new String[]{null};

    static ServerSettings baseSettings;
    static int            attrLength;
    JIRCD server;
    static Logger logger = LoggerFactory.getLogger(ServerTest.class);

    @BeforeEach
    void setUp(TestInfo info) {
        baseSettings = new ServerSettings();

        baseSettings.motd        = new String[0];
        baseSettings.host        = "jircd-host";
        baseSettings.networkName = "JIRCD";
        baseSettings.pass        = "jircd-pass";
        baseSettings.pingTimeout = TimeUnit.DAYS.toMillis(1);
        baseSettings.operators = new ArrayList<>(List.of(
                new ServerSettings.Operator("oper", "*", "oper"),
                new ServerSettings.Operator("googleOper", "google", "pass")
        ));

        logger.info("Creating server with settings: {}", baseSettings);
        boolean retry = true;
        while (retry && server == null) {
            try {
                server     = new JIRCD(baseSettings);
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
              .filter(field -> !Modifier.isFinal(field.getModifiers()))
              .forEach(field -> {
                  field.setAccessible(true);
                  try {
                      field.set(server.settings(), field.get(settings));
                  } catch (IllegalAccessException e) {
                      logger.error(e.getLocalizedMessage(), e);
                  }
              });
    }

    @AfterEach
    void tearDown(TestInfo info) {
        logger.info("{} test end", info.getDisplayName());
        server.shutdown();
    }

    @Nested
    class SocketTest {

        Connection[] connections;

        Connection createConnection() throws IOException {
            Socket client = new Socket("127.0.0.1", baseSettings.port);
            client.setSoTimeout(TIMEOUT_WHEN_WAITING_RESPONSE);
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

        public Optional<Channel> getChannel(String name) {
            return new ArrayList<>(server.originalChannels()).stream().filter(c -> c.name().equals(name)).findFirst();
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
        void fullTest() {
            addConnections(2);
            connections[0].send("PASS " + baseSettings.pass);
            connections[0].send("NICK bob");
            connections[0].send("USER bobby 0 * :Mobbye Plav");
            assertArrayEquals(new String[]{
                    ":jircd-host 001 bob :Welcome to the JIRCD Network, bob",
                    ":jircd-host 002 bob :Your host is %s, running version %s".formatted(Constant.NAME,
                                                                                         Constant.VERSION),
                    ":jircd-host 003 bob :This server was created %tD %tT".formatted(server.createdAt(),
                                                                                     server.createdAt()),
                    ":jircd-host 004 bob %s %s %s %s".formatted(Constant.NAME, Constant.VERSION, "", ""),
            }, connections[0].awaitMessage(4));
            int              count = 0;
            SupportAttribute attr  = server.supportAttribute();
            for (int i = 0; i < Math.max(Math.ceil(attr.length() / 13.), 1); i++) {
                count++;
                String[] messages = connections[0].awaitMessage();
                if (messages.length == 0) {
                    continue;
                }
                String isSupport = messages[0];
                assertTrue(isSupport.startsWith(":jircd-host 005 bob "));
                assertTrue(isSupport.endsWith(":are supported by this server"));
                isSupport = isSupport.replaceFirst(":jircd-host 005 bob ", "")
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
            assertArrayEquals(new String[]{
                    ":jircd-host 422 bob :MOTD File is missing"
            }, connections[0].awaitMessage());
            assertEquals(Math.max(Math.ceil(attr.length() / 13.), 1), count);
            UserInfo info = server.users().get(0).info();
            assertEquals("127.0.0.1", info.host());
            assertEquals("bob", info.nickname());
            assertEquals("bobby", info.username());
            assertEquals("Mobbye Plav", info.realName());
            assertEquals("bob", info.format());

            connections[0].send("JOIN #bob-sunhat");
            assertArrayEquals(new String[]{
                    ":bob JOIN #bob-sunhat",
                    ":jircd-host 353 bob = #bob-sunhat :~bob",
                    ":jircd-host 366 bob #bob-sunhat :End of /NAMES list"
            }, connections[0].awaitMessage(3));
            Optional<Channel> bobSunhatOpt = getChannel("#bob-sunhat");
            assertTrue(bobSunhatOpt.isPresent());
            Channel bobSunhat = bobSunhatOpt.get();

            connections[0].send("TOPIC #bob-sunhat :We talk about bob sunhat");
            assertArrayEquals(new String[]{
                    ":jircd-host 332 bob #bob-sunhat :We talk about bob sunhat"
            }, connections[0].awaitMessage());
            assertTrue(bobSunhat.topic().isPresent());

            connections[1].createUser("john", "John Doe");
            connections[1].send("LIST");
            assertArrayEquals(new String[]{
                    ":jircd-host 321 john Channel :Users  Name",
                    ":jircd-host 322 john #bob-sunhat 1 :We talk about bob sunhat",
                    ":jircd-host 323 john :End of /LIST"
            }, connections[1].awaitMessage(3));
            connections[1].send("JOIN #bob-sunhat");
            assertArrayEquals(new String[]{
                    ":john JOIN #bob-sunhat",
                    ":jircd-host 332 john #bob-sunhat :We talk about bob sunhat",
                    ":jircd-host 353 john = #bob-sunhat :~bob john",
                    ":jircd-host 366 john #bob-sunhat :End of /NAMES list"
            }, connections[1].awaitMessage(5));
            assertArrayEquals(new String[]{
                    ":john JOIN #bob-sunhat"
            }, connections[0].awaitMessage());

            connections[1].send("JOIN #joker");
            assertArrayEquals(new String[]{
                    ":john JOIN #joker",
                    ":jircd-host 353 john = #joker :~john",
                    ":jircd-host 366 john #joker :End of /NAMES list"
            }, connections[1].awaitMessage(3));
            Optional<Channel> jokerOpt = getChannel("#joker");
            assertTrue(jokerOpt.isPresent());
            Channel joker = jokerOpt.get();

            connections[2].createUser("fred", "Fred Bloggs");
            connections[2].send("LIST >0,<5");
            assertArrayEquals(new String[]{
                    ":jircd-host 321 fred Channel :Users  Name",
                    ":jircd-host 322 fred #bob-sunhat 2 :We talk about bob sunhat",
                    ":jircd-host 322 fred #joker 1 :",
                    ":jircd-host 323 fred :End of /LIST"
            }, connections[2].awaitMessage(4));
            connections[2].send("JOIN #bob-sunhat,#joker");
            assertArrayEquals(new String[]{
                    ":fred JOIN #bob-sunhat",
                    ":jircd-host 332 fred #bob-sunhat :We talk about bob sunhat",
                    ":jircd-host 353 fred = #bob-sunhat :~bob john fred",
                    ":jircd-host 366 fred #bob-sunhat :End of /NAMES list",
                    ":fred JOIN #joker",
                    ":jircd-host 353 fred = #joker :~john fred",
                    ":jircd-host 366 fred #joker :End of /NAMES list"
            }, connections[2].awaitMessage(7));
            assertArrayEquals(new String[]{
                    ":fred JOIN #bob-sunhat"
            }, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":fred JOIN #bob-sunhat",
                    ":fred JOIN #joker"
            }, connections[1].awaitMessage(2));

            connections[0].send("PRIVMSG #bob-sunhat :Here you can buy a bob sunhat: " +
                                "https://www.amazon.fr/Bob-Game-Color-Pikachu-Lorenzo/dp/B077MMR9CN");
            assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":bob PRIVMSG #bob-sunhat :Here you can buy a bob sunhat: " +
                    "https://www.amazon.fr/Bob-Game-Color-Pikachu-Lorenzo/dp/B077MMR9CN"
            }, connections[1].awaitMessage());
            assertArrayEquals(new String[]{
                    ":bob PRIVMSG #bob-sunhat :Here you can buy a bob sunhat: " +
                    "https://www.amazon.fr/Bob-Game-Color-Pikachu-Lorenzo/dp/B077MMR9CN"
            }, connections[2].awaitMessage());

            connections[1].send("PRIVMSG #joker :Let me go out!");
            assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
            assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":john PRIVMSG #joker :Let me go out!"
            }, connections[2].awaitMessage());

            connections[1].send("PART #bob-sunhat");
            assertArrayEquals(new String[]{
                    ":john PART #bob-sunhat"
            }, connections[1].awaitMessage());
            assertArrayEquals(new String[]{
                    ":john PART #bob-sunhat"
            }, connections[0].awaitMessage());
            assertArrayEquals(new String[]{
                    ":john PART #bob-sunhat"
            }, connections[2].awaitMessage());

            connections[1].send("QUIT");
            assertArrayEquals(SOCKET_CLOSE, connections[1].awaitMessage());
            assertArrayEquals(new String[]{
                    ":john QUIT :Quit: "
            }, connections[2].awaitMessage());
            assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());

            connections[2].send("QUIT :I'm left");
            assertArrayEquals(SOCKET_CLOSE, connections[2].awaitMessage());
            assertArrayEquals(new String[]{
                    ":fred QUIT :Quit: I'm left"
            }, connections[0].awaitMessage());
            assertTrue(joker.users().isEmpty());
            assertTrue(getChannel("#joker").isEmpty());

            connections[0].send("QUIT :Test ended");
            assertArrayEquals(SOCKET_CLOSE, connections[0].awaitMessage());
            assertTrue(bobSunhat.users().isEmpty());
            assertTrue(getChannel("#bob-sunhat").isEmpty());
            assertTrue(waitFor(() -> server.users().isEmpty()));
        }

        private String getRandomString(int length) {
            return getRandomString(length, Charset.defaultCharset());
        }

        private String getRandomString(int length, Charset charset) {
            return getRandomString(length, i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97), charset);
        }

        private String getRandomString(int length, IntPredicate filter) {
            return getRandomString(length, filter, Charset.defaultCharset());
        }

        private String getRandomString(int length, IntPredicate filter, Charset charset) {
            return getRandomString(length, 48, 123, filter, charset);
        }

        private String getRandomString(int length, int origin, int bound, IntPredicate filter) {
            return getRandomString(length, origin, bound, filter, Charset.defaultCharset());
        }

        private String getRandomString(int length, int origin, int bound, IntPredicate filter, Charset charset) {
            return new String(new Random().ints(origin, bound)
                               .filter(filter)
                               .limit(length)
                               .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                               .toString().getBytes(), charset);
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

            public String[] awaitMessage() {
                return awaitMessage(1);
            }

            public String[] awaitMessage(int count) {
                List<String> messages = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    try {
                        messages.add(input.readLine());
                    } catch (SocketTimeoutException ignored) {
                    } catch (IOException e) {
                        logger.error(e.getLocalizedMessage(), e);
                    }
                }
                return messages.toArray(String[]::new);
            }
        }

        @Nested
        class ConnectionMessage {

            @FullModuleTest
            void fullConnectionTest() {
                connections[0].send("PASS " + baseSettings.pass);
                assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
                connections[0].send("NICK bob");
                assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
                connections[0].send("USER bobby 0 * :Mobbye Plav");

                assertArrayEquals(new String[]{
                        ":jircd-host 001 bob :Welcome to the JIRCD Network, bob",
                        ":jircd-host 002 bob :Your host is %s, running version %s".formatted(Constant.NAME,
                                                                                             Constant.VERSION),
                        ":jircd-host 003 bob :This server was created %tD %tT".formatted(server.createdAt(),
                                                                                         server.createdAt()),
                        ":jircd-host 004 bob %s %s %s %s".formatted(Constant.NAME, Constant.VERSION, "", ""),
                }, connections[0].awaitMessage(4));

                SupportAttribute attr = server.supportAttribute();
                for (int i = 0; i < attrLength; i++) {
                    String[] messages = connections[0].awaitMessage();
                    if (messages.length == 0) {
                        continue;
                    }
                    String isSupport = messages[0];
                    assertTrue(isSupport.startsWith(":jircd-host 005 bob "));
                    assertTrue(isSupport.endsWith(":are supported by this server"));
                    isSupport = isSupport.replaceFirst(":jircd-host 005 bob ", "")
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

                assertArrayEquals(new String[]{
                        ":jircd-host 422 bob :MOTD File is missing"
                }, connections[0].awaitMessage());
                User bob = server.users().get(0);
                assertEquals(UserState.LOGGED, bob.state());

                connections[0].send("QUIT");
                assertTrue(waitFor(() -> bob.state() == UserState.DISCONNECTED, 1, TimeUnit.SECONDS));
            }

            @FullModuleTest
            void fullConnectionWithMOTDTest() throws IOException {
                Path tempFile = Files.createTempFile("motd", ".txt");
                Files.writeString(tempFile, "Custom motd set in temp file");

                setSettings(baseSettings.copy(new ServerSettings(tempFile), field -> !field.getName().equals("motd")));

                connections[0].send("PASS " + baseSettings.pass);
                assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());

                connections[0].send("NICK bob");
                assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());

                connections[0].send("USER bobby 0 * :Mobbye Plav");
                assertArrayEquals(new String[]{
                        ":jircd-host 001 bob :Welcome to the JIRCD Network, bob",
                        ":jircd-host 002 bob :Your host is %s, running version %s".formatted(Constant.NAME,
                                                                                             Constant.VERSION),
                        ":jircd-host 003 bob :This server was created %tD %tT".formatted(server.createdAt(),
                                                                                         server.createdAt()),
                        ":jircd-host 004 bob %s %s %s %s".formatted(Constant.NAME, Constant.VERSION, "", ""),
                }, connections[0].awaitMessage(4));

                SupportAttribute attr = server.supportAttribute();
                for (int i = 0; i < attrLength; i++) {
                    String[] messages = connections[0].awaitMessage();
                    if (messages.length == 0) {
                        continue;
                    }
                    String isSupport = messages[0];
                    assertTrue(isSupport.startsWith(":jircd-host 005 bob "));
                    assertTrue(isSupport.endsWith(":are supported by this server"));
                    isSupport = isSupport.replaceFirst(":jircd-host 005 bob ", "")
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

                assertArrayEquals(new String[]{
                        ":jircd-host 375 bob :- jircd-host Message of the day - ",
                        ":jircd-host 372 bob :Custom motd set in temp file",
                        ":jircd-host 376 bob :End of /MOTD command."
                }, connections[0].awaitMessage(3));
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
                    assertEquals(0, connections[0].awaitMessage().length);
                    UserInfo info = server.users().get(0).info();
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
                            ":jircd-host 461 @127.0.0.1 PASS :Not enough parameters"
                    }, connections[0].awaitMessage());
                    assertFalse(server.users().get(0).info().passwordValid());
                }

                @Test
                void incorrectPassTest() {
                    String passwd = getRandomString(new Random().nextInt(9) + 1);
                    assumeFalse(baseSettings.pass.equals(passwd));
                    connections[0].send("PASS " + passwd);
                    assertArrayEquals(new String[]{
                            ":jircd-host 464 @127.0.0.1 :Password incorrect"
                    }, connections[0].awaitMessage());
                    assertFalse(server.users().get(0).info().passwordValid());
                }

                @Test
                void alreadyRegisteredPassTest() {
                    connections[0].createUser("john", "John Doe");
                    connections[0].send("PASS " + baseSettings.pass);
                    assertArrayEquals(new String[]{
                            ":jircd-host 462 john :You may not reregister"
                    }, connections[0].awaitMessage());
                }

            }

            @Nested
            class NickCommand {
                @Test
                void nickTest() throws InterruptedException {
                    connections[0].send("PASS " + baseSettings.pass);
                    Thread.sleep(TIMEOUT_BETWEEN_COMMUNICATION);

                    connections[0].send("NICK bob");
                    Thread.sleep(TIMEOUT_BETWEEN_COMMUNICATION);
                    UserInfo info = server.users().get(0).info();
                    assertTrue(info.passwordValid());
                    assertEquals("bob", info.nickname());
                    assertArrayEquals(new String[]{}, connections[0].awaitMessage());

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
                            ":jircd-host 432 @127.0.0.1 " + nickname + " :Erroneus nickname"
                    }, connections[0].awaitMessage());
                }

                @Test
                void incorrectNickTest() {
                    connections[0].send("PASS " + baseSettings.pass);
                    String nickname = getRandomString(7, 160, 255, i -> true);
                    connections[0].send("NICK " + nickname);
                    assertArrayEquals(new String[]{
                            ":jircd-host 432 @127.0.0.1 " + nickname + " :Erroneus nickname"
                    }, connections[0].awaitMessage());
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
                            ":jircd-host 433 @127.0.0.1 dup :Nickname is already in use"
                    }, connections[1].awaitMessage());
                }

                @Test
                void unsafeNickWithSafenetTest() throws InterruptedException {
                    connections[0].send("PASS " + baseSettings.pass);
                    String nick = ListUtils.getRandom(baseSettings.unsafeNickname);
                    connections[0].send("NICK " + nick);
                    Thread.sleep(TIMEOUT_BETWEEN_COMMUNICATION);

                    UserInfo info = server.users().get(0).info();
                    assertTrue(info.passwordValid());
                    assertEquals(nick, info.nickname());
                    assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
                }

                @Test
                void unsafeNickWithUnsafenetTest() throws InterruptedException {
                    connections[0].send("PASS " + baseSettings.pass);
                    Thread.sleep(TIMEOUT_BETWEEN_COMMUNICATION);
                    String nick = ListUtils.getRandom(baseSettings.unsafeNickname);

                    UserInfo info = server.users().get(0).info();
                    info.setHost("255.255.255.255");

                    connections[0].send("NICK " + nick);
                    assertArrayEquals(new String[]{
                            ":jircd-host 432 @255.255.255.255 " + nick + " :Erroneus nickname"
                    }, connections[0].awaitMessage());
                    assertNull(info.nickname());
                }
            }

            @Nested
            class UserCommand {
                @Test
                void userTest() {
                    connections[0].send("PASS " + baseSettings.pass);
                    connections[0].send("USER bobby 0 * :Mobbye Plav" + ENDING);
                    assertTrue(waitFor(500, TimeUnit.MILLISECONDS));
                    UserInfo info = server.users().get(0).info();
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
                            ":jircd-host 462 bob :You may not reregister"
                    }, connections[0].awaitMessage());
                }
            }

            @Nested
            class OperCommand {
                @Test
                void operTest() {
                    ServerSettings.Operator savedOper = baseSettings.operators.get(0);
                    connections[0].send("OPER " + savedOper.username() + " " + savedOper.password());
                    assertArrayEquals(new String[]{
                            ":jircd-host 381 @127.0.0.1 :You are now an IRC operator"
                    }, connections[0].awaitMessage());
                }

                @Test
                void incorrectPasswdOperTest() {
                    ServerSettings.Operator savedOper = baseSettings.operators.get(0);
                    connections[0].send(
                            "OPER " + savedOper.username() + " " + getRandomString(new Random().nextInt(9) + 1));
                    assertArrayEquals(new String[]{
                            ":jircd-host 464 @127.0.0.1 :Password incorrect"
                    }, connections[0].awaitMessage());
                }

                @Test
                void incorrectParamsNumberOperTest() {
                    connections[0].send("OPER");
                    assertArrayEquals(new String[]{
                            ":jircd-host 461 @127.0.0.1 OPER :Not enough parameters"
                    }, connections[0].awaitMessage());
                }

                @Test
                void incorrectOperHostTest() {
                    connections[0].send("OPER " + baseSettings.operators.get(1).username() + " " +
                                        baseSettings.operators.get(1).password());
                    assertArrayEquals(new String[]{
                            ":jircd-host 491 @127.0.0.1 :No O-lines for your host"
                    }, connections[0].awaitMessage());
                }
            }

            @Nested
            class QuitCommand {
                @Test
                void quitTest() throws InterruptedException {
                    Thread.sleep(TIMEOUT_BETWEEN_COMMUNICATION);
                    assumeTrue(waitFor(() -> server.users().size() == 1));
                    User user = server.users().get(0);
                    connections[0].send("QUIT");

                    Thread.sleep(TIMEOUT_BETWEEN_COMMUNICATION);
                    assertTrue(server.users().isEmpty());
                    assertEquals(UserState.DISCONNECTED, user.state());
                }

                @Test
                void quitWithReasonTest() throws InterruptedException {
                    Thread.sleep(TIMEOUT_BETWEEN_COMMUNICATION);
                    assumeTrue(waitFor(() -> server.users().size() == 1));
                    User user = server.users().get(0);
                    connections[0].send("QUIT :Bye");

                    Thread.sleep(TIMEOUT_BETWEEN_COMMUNICATION);
                    assertTrue(server.users().isEmpty());
                    assertEquals(UserState.DISCONNECTED, user.state());
                }
            }
        }

        @Nested
        class ChannelOperation {

            @BeforeEach
            void setUp() {
                connections[0].createUser("bob", "bobby", "Mobbye Plav");
            }

            @FullModuleTest
            void fullChannelOperationTest() {
                assumeTrue(server.channels().isEmpty());
                connections[0].send("LIST");
                assertArrayEquals(new String[]{
                        ":jircd-host 321 bob Channel :Users  Name",
                        ":jircd-host 323 bob :End of /LIST"
                }, connections[0].awaitMessage(2));

                connections[0].send("JOIN #jircd");
                assertArrayEquals(new String[]{
                        ":bob JOIN #jircd",
                        ":jircd-host 353 bob = #jircd :~bob",
                        ":jircd-host 366 bob #jircd :End of /NAMES list"
                }, connections[0].awaitMessage(3));
                assertFalse(server.channels().isEmpty());
                Optional<Channel> jircdOpt = getChannel("#jircd");
                assertFalse(jircdOpt.isEmpty());
                Channel jircd = jircdOpt.get();
                assertFalse(jircd.users().isEmpty());

                assertTrue(jircd.topic().isEmpty());
                connections[0].send("TOPIC #jircd :A java internet relay chat deamon");
                assertArrayEquals(new String[]{
                        ":jircd-host 332 bob #jircd :A java internet relay chat deamon"
                }, connections[0].awaitMessage());
                assertTrue(jircd.topic().isPresent());

                connections[0].send("NAMES #jircd");
                assertArrayEquals(new String[]{
                        ":jircd-host 353 bob = #jircd :~bob",
                        ":jircd-host 366 bob #jircd :End of /NAMES list"
                }, connections[0].awaitMessage(2));

                connections[0].send("LIST");
                assertArrayEquals(new String[]{
                        ":jircd-host 321 bob Channel :Users  Name",
                        ":jircd-host 322 bob #jircd 1 :A java internet relay chat deamon",
                        ":jircd-host 323 bob :End of /LIST"
                }, connections[0].awaitMessage(3));

                connections[0].send("PART #jircd");
                assertArrayEquals(new String[]{
                        ":bob PART #jircd"
                }, connections[0].awaitMessage());
                assumeTrue(waitFor(1, TimeUnit.SECONDS));
                assertTrue(getChannel("#jircd").isEmpty());
            }

            @Nested
            class JoinCommand {

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
                    }, connections[1].awaitMessage(4));
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
                    }, connections[1].awaitMessage(4));
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

                    connections[1].send("PASS " + baseSettings.pass);
                    connections[1].send("NICK bab");
                    connections[1].send("USER babby 0 * :MAbbye Plov");
                    connections[1].ignoreMessage(6 + attrLength + baseSettings.motd.length);
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
                    channel.modes()._protected(true);

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
                            ":jircd-host 321 john Channel :Users  Name",
                            ":jircd-host 322 john #hello 1 :",
                            ":jircd-host 322 john #bonjour 1 :",
                            ":jircd-host 322 john #hey 1 :",
                            ":jircd-host 322 john #bye 1 :",
                            ":jircd-host 323 john :End of /LIST"
                    }, connections[1].awaitMessage(6));
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
                            ":jircd-host 321 ann Channel :Users  Name",
                            ":jircd-host 322 ann #hello 3 :",
                            ":jircd-host 322 ann #bonjour 4 :",
                            ":jircd-host 322 ann #bye 3 :",
                            ":jircd-host 323 ann :End of /LIST"
                    }, connections[4].awaitMessage(5));
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
                            ":jircd-host 321 ann Channel :Users  Name",
                            ":jircd-host 322 ann #hey 1 :",
                            ":jircd-host 323 ann :End of /LIST"
                    }, connections[4].awaitMessage(3));
                }

                @Test
                void listWithMask() {
                    connections[0].send("JOIN #42,#azerty,#AZERTY,#1abc2");
                    connections[0].ignoreMessage(3 * 4);

                    connections[1].send("LIST #[0-9]*");
                    assertArrayEquals(new String[]{
                            ":jircd-host 321 john Channel :Users  Name",
                            ":jircd-host 323 john :End of /LIST"
                    }, connections[1].awaitMessage(2));

                    connections[1].send("LIST #[0-9][abc]{3}[0-9]");
                    assertArrayEquals(new String[]{
                            ":jircd-host 321 john Channel :Users  Name",
                            ":jircd-host 323 john :End of /LIST"
                    }, connections[1].awaitMessage(2));

                    connections[1].send("LIST #[a-z]*");
                    assertArrayEquals(new String[]{
                            ":jircd-host 321 john Channel :Users  Name",
                            ":jircd-host 323 john :End of /LIST"
                    }, connections[1].awaitMessage(2));

                    connections[1].send("LIST #[A-Z]*");
                    assertArrayEquals(new String[]{
                            ":jircd-host 321 john Channel :Users  Name",
                            ":jircd-host 323 john :End of /LIST"
                    }, connections[1].awaitMessage(2));

                    connections[1].send("LIST #*");
                    assertArrayEquals(new String[]{
                            ":jircd-host 321 john Channel :Users  Name",
                            ":jircd-host 322 john #42 1 :",
                            ":jircd-host 322 john #azerty 1 :",
                            ":jircd-host 322 john #AZERTY 1 :",
                            ":jircd-host 322 john #1abc2 1 :",
                            ":jircd-host 323 john :End of /LIST"
                    }, connections[1].awaitMessage(6));

                    connections[1].send("LIST #");
                    assertArrayEquals(new String[]{
                            ":jircd-host 321 john Channel :Users  Name",
                            ":jircd-host 322 john #42 1 :",
                            ":jircd-host 322 john #azerty 1 :",
                            ":jircd-host 322 john #AZERTY 1 :",
                            ":jircd-host 322 john #1abc2 1 :",
                            ":jircd-host 323 john :End of /LIST"
                    }, connections[1].awaitMessage(6));

                    connections[1].send("LIST 2");
                    assertArrayEquals(new String[]{
                            ":jircd-host 321 john Channel :Users  Name",
                            ":jircd-host 322 john #42 1 :",
                            ":jircd-host 322 john #1abc2 1 :",
                            ":jircd-host 323 john :End of /LIST"
                    }, connections[1].awaitMessage(4));
                }
            }

            @Nested
            class KickCommand {

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
                    connections[0].send("KICK "+channel+" fred");
                    assertArrayEquals(new String[]{
                            ":jircd-host 403 bob "+channel+" :No such channel"
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

                    Optional<User> userOpt = server.users()
                                                   .stream()
                                                   .filter(u -> u.info().nickname().equals("fred"))
                                                   .findFirst();
                    assertFalse(userOpt.isEmpty());
                    User user = userOpt.get();
                    channel.prefix(user, Channel.Rank.PROTECTED.prefix+"");

                    assumeTrue(channel.users().size() == 2);
                    assumeTrue(channel.isRanked(user, Channel.Rank.PROTECTED));

                    connections[0].send("KICK #jircd fred");
                    assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());

                    assertEquals(2, channel.users().size());
                }
            }
        }

        @Nested
        class ServerQueriesAndCommands {

            @BeforeEach
            void setUp() {
                connections[0].createUser("bob", "bobby", "Mobbye Plav");
            }

            @FullModuleTest
            @Disabled
            void fullServerQueriesAndCommandsTest() {

            }

            @Nested
            class MotdCommand {
                @Test
                void motdWithUnknownServerTest() {
                    connections[0].send("MOTD UnknownServer");
                    assertArrayEquals(new String[]{
                            ":jircd-host 402 bob UnknownServer :No such server"
                    }, connections[0].awaitMessage());
                }

                @Test
                void motdWithNoMOTDTest() {
                    assumeTrue(server.settings().motd.length == 0);
                    connections[0].send("MOTD");
                    assertArrayEquals(new String[]{
                            ":jircd-host 422 bob :MOTD File is missing"
                    }, connections[0].awaitMessage());
                }

                @Test
                void motdTest() throws IOException {
                    Path tempFile = Files.createTempFile("motd", ".txt");
                    Files.writeString(tempFile, "Custom motd set in temp file");

                    setSettings(
                            baseSettings.copy(new ServerSettings(tempFile), field -> !field.getName().equals("motd")));

                    assumeFalse(server.settings().motd.length == 0);
                    connections[0].send("MOTD");
                    assertArrayEquals(new String[]{
                            ":jircd-host 375 bob :- jircd-host Message of the day - ",
                            ":jircd-host 372 bob :Custom motd set in temp file",
                            ":jircd-host 376 bob :End of /MOTD command."
                    }, connections[0].awaitMessage(3));
                }
            }

            @Nested
            class VersionCommand {
                @Test
                void versionWithUnknownServerTest() {
                    connections[0].send("VERSION UnknownServer");
                    assertArrayEquals(new String[]{
                            ":jircd-host 402 bob UnknownServer :No such server"
                    }, connections[0].awaitMessage());
                }

                @Test
                void versionTest() {
                    connections[0].send("VERSION");

                    assertArrayEquals(new String[]{
                            ":jircd-host 351 bob %s jircd-host :".formatted(Constant.VERSION)
                    }, connections[0].awaitMessage());

                    SupportAttribute attr = server.supportAttribute();
                    for (int i = 0; i < attrLength; i++) {
                        String[] messages = connections[0].awaitMessage();
                        if (messages.length == 0) {
                            continue;
                        }
                        String isSupport = messages[0];
                        assertTrue(isSupport.startsWith(":jircd-host 005 bob "));
                        assertTrue(isSupport.endsWith(":are supported by this server"));
                        isSupport = isSupport.replaceFirst(":jircd-host 005 bob ", "")
                                             .replace(" :are supported by this server", "");

                        String[] attributes = isSupport.split(" ");
                        assertTrue(attributes.length <= 13);
                        for (String attribute : attributes) {
                            String key = attribute.contains("=") ?
                                    attribute.split("=")[0] :
                                    attribute;
                            String value = attribute.contains("=") ?
                                    attribute.split("=")[1] :
                                    null;
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
                                    expectedClazz
                                                  = (Class<?>) ((ParameterizedType) SupportAttribute.class.getDeclaredField(
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
                }
            }

            @Nested
            class AdminCommand {
                @Test
                void adminWithUnknownServerTest() {
                    connections[0].send("ADMIN UnknownServer");
                    assertArrayEquals(new String[]{
                            ":jircd-host 402 bob UnknownServer :No such server"
                    }, connections[0].awaitMessage());
                }

                @Test
                void adminTest() {
                    server.settings().admin = new ServerSettings.Admin(
                            "Location 1",
                            "Location 2",
                            "jircd@local.host"
                    );
                    connections[0].send("ADMIN");

                    assertArrayEquals(new String[]{
                            ":jircd-host 256 bob jircd-host :Administrative info",
                            ":jircd-host 257 bob :Location 1",
                            ":jircd-host 258 bob :Location 2",
                            ":jircd-host 259 bob :jircd@local.host"
                    }, connections[0].awaitMessage(4));
                }
            }

            @Nested
            class ConnectCommand {
                @Test
                void connectErrorTest() {
                    connections[0].send("CONNECT one");
                    assertArrayEquals(new String[]{
                            ":jircd-host 400 bob CONNECT :Not supported yet"
                    }, connections[0].awaitMessage());

                    connections[0].send("CONNECT one two");
                    assertArrayEquals(new String[]{
                            ":jircd-host 400 bob CONNECT :Not supported yet"
                    }, connections[0].awaitMessage());

                    connections[0].send("CONNECT one two three");
                    assertArrayEquals(new String[]{
                            ":jircd-host 400 bob CONNECT :Not supported yet"
                    }, connections[0].awaitMessage());
                }

                @Test
                @Disabled("Not supported yet")
                void connectOneArgumentTest() {
                    connections[0].send("CONNECT one");
                }

                @Test
                @Disabled("Not supported yet")
                void connectTwoArgumentTest() {
                    connections[0].send("CONNECT one two");
                }

                @Test
                @Disabled("Not supported yet")
                void connectThreeArgumentTest() {
                    connections[0].send("CONNECT one two three");
                }
            }

            @Nested
            class LUserCommand {
                @Test
                void luserTest() {
                    connections[0].send("LUSER");
                    assertArrayEquals(new String[]{
                            ":jircd-host 251 bob :There are 1 users and 0 invisibles on 1 servers",
                            ":jircd-host 252 bob 0 :operator(s) online",
                            ":jircd-host 253 bob 0 :unknown connection(s)",
                            ":jircd-host 254 bob 0 :channels formed",
                            ":jircd-host 255 bob :I have 1 clients and 1 servers"
                    }, connections[0].awaitMessage(5));
                }

                @Test
                void luserTestWithUnknown() {
                    addConnections(1);

                    connections[0].send("LUSER");
                    assertArrayEquals(new String[]{
                            ":jircd-host 251 bob :There are 1 users and 0 invisibles on 1 servers",
                            ":jircd-host 252 bob 0 :operator(s) online",
                            ":jircd-host 253 bob 1 :unknown connection(s)",
                            ":jircd-host 254 bob 0 :channels formed",
                            ":jircd-host 255 bob :I have 2 clients and 1 servers"
                    }, connections[0].awaitMessage(5));
                }

                @Test
                void luserTestWithChannel() {
                    connections[0].send("JOIN #jircd");
                    connections[0].ignoreMessage(3);

                    assumeTrue(server.channels().size() == 1);

                    connections[0].send("LUSER");
                    assertArrayEquals(new String[]{
                            ":jircd-host 251 bob :There are 1 users and 0 invisibles on 1 servers",
                            ":jircd-host 252 bob 0 :operator(s) online",
                            ":jircd-host 253 bob 0 :unknown connection(s)",
                            ":jircd-host 254 bob 1 :channels formed",
                            ":jircd-host 255 bob :I have 1 clients and 1 servers"
                    }, connections[0].awaitMessage(5));
                }

                @Test
                void luserTestWithInvisible() {
                    server.users().get(0).modes().invisible(true);
                    assumeTrue(server.users().get(0).modes().invisible());

                    connections[0].send("LUSER");
                    assertArrayEquals(new String[]{
                            ":jircd-host 251 bob :There are 0 users and 1 invisibles on 1 servers",
                            ":jircd-host 252 bob 0 :operator(s) online",
                            ":jircd-host 253 bob 0 :unknown connection(s)",
                            ":jircd-host 254 bob 0 :channels formed",
                            ":jircd-host 255 bob :I have 1 clients and 1 servers"
                    }, connections[0].awaitMessage(5));
                }

                @Test
                void luserTestWithOper() {
                    connections[0].send("OPER "+baseSettings.operators.get(0).username()+" "+baseSettings.operators.get(0).password());
                    connections[0].ignoreMessage();
                    assumeTrue(server.users().get(0).modes().oper());

                    connections[0].send("LUSER");
                    assertArrayEquals(new String[]{
                            ":jircd-host 251 bob :There are 1 users and 0 invisibles on 1 servers",
                            ":jircd-host 252 bob 1 :operator(s) online",
                            ":jircd-host 253 bob 0 :unknown connection(s)",
                            ":jircd-host 254 bob 0 :channels formed",
                            ":jircd-host 255 bob :I have 1 clients and 1 servers"
                    }, connections[0].awaitMessage(5));
                }
            }

            @Nested
            class TimeCommand {
                @Test
                void timeWithUnknownServerTest() {
                    connections[0].send("TIME UnknownServer");
                    assertArrayEquals(new String[]{
                            ":jircd-host 402 bob UnknownServer :No such server"
                    }, connections[0].awaitMessage());
                }

                @Test
                void timeTest() {
                    connections[0].send("TIME");

                    assertArrayEquals(new String[]{
                            ":jircd-host 391 jircd-host :%s".formatted(
                                    DateTimeFormatter.ofPattern("EEEE LLLL dd yyyy - HH:mm O", Locale.ENGLISH)
                                                     .format(ZonedDateTime.now()))
                    }, connections[0].awaitMessage());
                }
            }

            @Nested
            class StatsCommand {
                @Test
                @Disabled("No completed")
                void statsCTest() {
                    connections[0].send("STATS c");
                }

                @Test
                @Disabled("No completed")
                void statsHTest() {
                    connections[0].send("STATS h");
                }

                @Test
                @Disabled("No completed")
                void statsITest() {
                    connections[0].send("STATS i");
                }

                @Test
                @Disabled("No completed")
                void statsKTest() {
                    connections[0].send("STATS k");
                }

                @Test
                @Disabled("No completed")
                void statsLTest() {
                    connections[0].send("STATS l");
                }

                @Test
                void statsMTest() {
                    connections[0].send("STATS m");
                    assertArrayEquals(new String[]{
                            ":jircd-host 212 ADMIN 0",
                            ":jircd-host 212 CONNECT 0",
                            ":jircd-host 212 HELP 0",
                            ":jircd-host 212 INFO 0",
                            ":jircd-host 212 JOIN 0",
                            ":jircd-host 212 KICK 0",
                            ":jircd-host 212 KILL 0",
                            ":jircd-host 212 LIST 0",
                            ":jircd-host 212 LUSER 0",
                            ":jircd-host 212 MODE 0",
                            ":jircd-host 212 MOTD 0",
                            ":jircd-host 212 NAMES 0",
                            ":jircd-host 212 NICK 1",
                            ":jircd-host 212 NOTICE 0",
                            ":jircd-host 212 OPER 0",
                            ":jircd-host 212 PART 0",
                            ":jircd-host 212 PASS 1",
                            ":jircd-host 212 PING 0",
                            ":jircd-host 212 PRIVMSG 0",
                            ":jircd-host 212 QUIT 0",
                            ":jircd-host 212 STATS 1",
                            ":jircd-host 212 TIME 0",
                            ":jircd-host 212 TOPIC 0",
                            ":jircd-host 212 USER 1",
                            ":jircd-host 212 USERHOST 0",
                            ":jircd-host 212 VERSION 0",
                            ":jircd-host 219 M :End of /STATS report"
                    }, connections[0].awaitMessage(27));
                }

                @Test
                @Disabled("No completed")
                void statsOTest() {
                    connections[0].send("STATS o");
                }

                @Test
                void statsUTest() {
                    Pattern pat = Pattern.compile("^:jircd-host 242 :Server Up 0 days 0:[0-5][0-9]:[0-5][0-9]$");

                    connections[0].send("STATS u");
                    assertTrue(pat.matcher(connections[0].awaitMessage()[0]).matches());
                    assertEquals(":jircd-host 219 U :End of /STATS report", connections[0].awaitMessage()[0]);

                    assertTrue(waitFor(72, TimeUnit.SECONDS));
                    connections[0].send("STATS u");
                    assertTrue(pat.matcher(connections[0].awaitMessage()[0]).matches());
                    assertEquals(":jircd-host 219 U :End of /STATS report", connections[0].awaitMessage()[0]);
                }

                @Test
                @Disabled("No completed")
                void statsYTest() {
                    connections[0].send("STATS y");
                }
            }

            @Nested
            class HelpCommand {

                @Test
                void helpNoSubjectTest() {
                    connections[0].send("HELP");
                    assertArrayEquals(new String[]{
                            ":jircd-host 524 bob :No help available on this topic",
                    }, connections[0].awaitMessage());
                }

                @Test
                void helpTest() {
                    connections[0].send("HELP subject");
                    assertArrayEquals(new String[]{
                            ":jircd-host 524 bob subject :No help available on this topic",
                    }, connections[0].awaitMessage());
                }
            }

            @Nested
            class InfoCommand {
                @Test
                void infoWithUnknownServerTest() {
                    connections[0].send("INFO UnknownServer");
                    assertArrayEquals(new String[]{
                            ":jircd-host 402 bob UnknownServer :No such server"
                    }, connections[0].awaitMessage());
                }

                @Test
                void infoTest() {
                    connections[0].send("INFO");
                    assertArrayEquals(new String[]{
                            ":jircd-host 371 :jircd v%s".formatted(Constant.VERSION),
                            ":jircd-host 371 :by Antoine <antoine@enimaloc.fr>",
                            ":jircd-host 371 :Source code: https://github.com/enimaloc/jircd",
                            ":jircd-host 374 :End of /INFO list"
                    }, connections[0].awaitMessage(4));
                }
            }

            @Nested
            class ModeCommand {

                @Nested
                class UserMode {

                    User user;

                    @BeforeEach
                    void setUp() {
                        assumeFalse(server.users().isEmpty());
                        assumeFalse((user = (User) server.users().get(0)) == null);
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
                        assumeTrue(user.modes().modes().isEmpty());
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
                            assertTrue(user.modes().modes().isEmpty());
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
                            assertTrue(user.modes().modes().isEmpty());
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
                        assumeTrue(channelOpt.isPresent());
                        this.channel = (Channel) channelOpt.get();
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
                            assumeFalse(channel.modes().bans().isEmpty());
                            connections[0].send("MODE #bob -b john!*@*");
                            assertArrayEquals(new String[]{
                                    ":bob MODE #bob -b john!*@*"
                            }, connections[0].awaitMessage());
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
                            assumeFalse(channel.modes().limit().isEmpty());
                            assumeTrue(channel.modes().limit().getAsInt() == 5);
                            connections[0].send("MODE #bob -l 5");
                            assertArrayEquals(new String[]{
                                    ":bob MODE #bob -l 5"
                            }, connections[0].awaitMessage());
                            assumeTrue(channel.modes().limit().isEmpty());
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
                            assumeFalse(channel.modes().password().isEmpty());
                            connections[0].send("MODE #bob -k keypass");
                            assertArrayEquals(new String[]{
                                    ":bob MODE #bob -k *"
                            }, connections[0].awaitMessage());
                            assertTrue(channel.modes().password().isEmpty());
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
            }
        }

        @Nested
        class SendingMessage {

            @BeforeEach
            void setUp() {
                connections[0].createUser("bob", "bobby", "Mobbye Plav");
                addConnections(1);
                connections[1].createUser("john", "John Doe");
            }

            @Nested
            class PrivmsgCommand {

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
                        ((User) server.users().get(1)).away("I'm not here for now");
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
                        ((Channel) channel).prefix(server.users().get(0), "+");
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
                        ((Channel) channel).prefix(fred, "@");

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
            }

            @Nested
            class NoticeCommand {

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
                    ((Channel) channel).prefix(server.users().get(0), "+");
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
            }
        }

        @Nested
        class OptionalMessage {

            @Nested
            class UserhostCommand {
                @BeforeEach
                void setUp() {
                    connections[0].createUser("bob", "bobby", "Mobbye Plav");
                }

                @Test
                void userhostWithOneParametersTest() {
                    addConnections(1);
                    connections[1].createUser("john", "John Doe");

                    connections[0].send("USERHOST john");
                    assertArrayEquals(new String[]{
                            ":jircd-host 302 bob :john+127.0.0.1"
                    }, connections[0].awaitMessage());
                }

                @Test
                void userhostWithTwoParametersTest() {
                    addConnections(2);
                    connections[1].createUser("john", "John Doe");
                    connections[2].createUser("fred", "Fred Bloggs");

                    connections[0].send("USERHOST john fred");
                    assertArrayEquals(new String[]{
                            ":jircd-host 302 bob :john+127.0.0.1 fred+127.0.0.1"
                    }, connections[0].awaitMessage());
                }

                @Test
                void userhostWithThreeParametersTest() {
                    addConnections(3);
                    connections[1].createUser("john", "John Doe");
                    connections[2].createUser("fred", "Fred Bloggs");
                    connections[3].createUser("tommy", "Tommy Atkins");

                    connections[0].send("USERHOST john fred tommy");
                    assertArrayEquals(new String[]{
                            ":jircd-host 302 bob :john+127.0.0.1 fred+127.0.0.1 tommy+127.0.0.1"
                    }, connections[0].awaitMessage());
                }

                @Test
                void userhostWithFourParametersTest() {
                    addConnections(4);
                    connections[1].createUser("john", "John Doe");
                    connections[2].createUser("fred", "Fred Bloggs");
                    connections[3].createUser("tommy", "Tommy Atkins");
                    connections[4].createUser("ann", "Ann Yonne");

                    connections[0].send("USERHOST john fred tommy ann");
                    assertArrayEquals(new String[]{
                            ":jircd-host 302 bob :john+127.0.0.1 fred+127.0.0.1 tommy+127.0.0.1 ann+127.0.0.1"
                    }, connections[0].awaitMessage());
                }

                @Test
                void userhostWithFiveParametersTest() {
                    addConnections(5);
                    connections[1].createUser("john", "John Doe");
                    connections[2].createUser("fred", "Fred Bloggs");
                    connections[3].createUser("tommy", "Tommy Atkins");
                    connections[4].createUser("ann", "Ann Yonne");
                    connections[5].createUser("ratman", "Doug Rattmann");

                    connections[0].send("USERHOST john fred tommy ann ratman");
                    assertArrayEquals(new String[]{
                            ":jircd-host 302 bob :john+127.0.0.1 fred+127.0.0.1 tommy+127.0.0.1 ann+127.0.0.1 ratman+127.0.0.1"
                    }, connections[0].awaitMessage());
                }

                @Test
                void userhostOperatorTest() {
                    addConnections(2);
                    connections[1].createUser("john", "John Doe");
                    assumeTrue(server.users().get(1) != null);
                    server.users().get(1).modes().oper(true);
                    connections[2].createUser("fred", "Fred Bloggs");

                    connections[0].send("USERHOST john fred");
                    assertArrayEquals(new String[]{
                            ":jircd-host 302 bob :john*+127.0.0.1 fred+127.0.0.1"
                    }, connections[0].awaitMessage());
                }

                @Test
                void userhostAwayTest() {
                    addConnections(2);
                    connections[1].createUser("john", "John Doe");
                    assumeTrue(server.users().get(1) != null);
                    ((User) server.users().get(1)).away("Away!");
                    connections[2].createUser("fred", "Fred Bloggs");

                    connections[0].send("USERHOST john fred");
                    assertArrayEquals(new String[]{
                            ":jircd-host 302 bob :john-127.0.0.1 fred+127.0.0.1"
                    }, connections[0].awaitMessage());
                }
            }
        }

        @Nested
        class MiscellaneousMessage {

            @Nested
            class KillCommand {

                @BeforeEach
                void setUp() {
                    addConnections(1);
                    connections[0].createUser("bob", "bobby", "Mobbye Plav");
                    connections[1].createUser("john", "John Doe");
                    connections[0].send("JOIN #kill");
                    connections[0].ignoreMessage(3);

                    connections[1].send("JOIN #kill");
                    connections[1].ignoreMessage(3);
                    connections[0].ignoreMessage();
                }

                @Test
                void killTest() {
                    assumeTrue(server.users().get(0) != null);
                    server.users().get(0).modes().oper(true);
                    connections[0].send("KILL john :Stop spamming");
                    assertArrayEquals(new String[]{
                            ":john QUIT :Quit: Killed (bob (Stop spamming))"
                    }, connections[0].awaitMessage());
                    assertArrayEquals(new String[]{
                            "Closing Link: jircd-host (Killed (bob (Stop spamming)))"
                    }, connections[1].awaitMessage());
                    assertArrayEquals(SOCKET_CLOSE, connections[1].awaitMessage());
                }

                @Test
                void killLocalOperTest() {
                    assumeTrue(server.users().get(0) != null);
                    server.users().get(0).modes().localOper(true);
                    connections[0].send("KILL john :Stop spamming");
                    assertArrayEquals(new String[]{
                            ":john QUIT :Quit: Killed (bob (Stop spamming))"
                    }, connections[0].awaitMessage());
                    assertArrayEquals(new String[]{
                            "Closing Link: jircd-host (Killed (bob (Stop spamming)))"
                    }, connections[1].awaitMessage());
                    assertArrayEquals(SOCKET_CLOSE, connections[1].awaitMessage());
                }

                @Test
                void killUnknownTest() {
                    assumeTrue(server.users().get(0) != null);
                    server.users().get(0).modes().localOper(true);
                    connections[0].send("KILL x :Stop spamming");
                    assertArrayEquals(EMPTY_ARRAY, connections[0].awaitMessage());
                    assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
                }

                @Test
                void killNotOperTest() {
                    connections[0].send("KILL john :Stop spamming");
                    assertArrayEquals(new String[]{
                            ":jircd-host 481 bob :Permission Denied- You're not an IRC operator"
                    }, connections[0].awaitMessage());
                    assertArrayEquals(EMPTY_ARRAY, connections[1].awaitMessage());
                }
            }
        }
    }
}
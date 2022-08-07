package fr.enimaloc.jircd;/*
 * fr.enimaloc.jircd.SocketTest
 *
 * 0.0.1
 *
 * 05/08/2022
 */

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.server.attributes.SupportAttribute;
import fr.enimaloc.jircd.user.User;
import fr.enimaloc.jircd.user.UserInfo;
import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import utils.FullModuleTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 *
 */
public class SocketBase extends ServerBase {

    protected Connection[] connections;

    protected Connection createConnection() throws IOException {
        Socket client = new Socket("127.0.0.1", baseSettings.port);
        client.setSoTimeout(TIMEOUT_WHEN_WAITING_RESPONSE);
        BufferedReader input = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));
        PrintStream output = new PrintStream(client.getOutputStream());
        return new Connection(System.currentTimeMillis(), client, input, output);
    }

    protected void addConnections(int number) {
        addConnections(number, true);
    }

    protected void addConnections(int number, boolean wait) {
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

        if (wait) {
            assumeTrue(waitFor(() -> server.users().size() == connections.length));
        }
    }

    protected boolean waitFor(int timeout, TimeUnit unit) {
        return !waitFor(() -> false, timeout, unit);
    }

    protected static boolean waitFor(BooleanSupplier condition) {
        return waitFor(condition, true);
    }

    protected static boolean waitFor(BooleanSupplier condition, boolean expected) {
        return waitFor(condition, expected, 5, TimeUnit.SECONDS);
    }

    protected static boolean waitFor(BooleanSupplier condition, int timeout, TimeUnit unit) {
        return waitFor(condition, true, timeout, unit);
    }

    protected static boolean waitFor(BooleanSupplier condition, boolean expected, int timeout, TimeUnit unit) {
        long timedOut = System.currentTimeMillis() + unit.toMillis(timeout);
        while (condition.getAsBoolean() != expected) {
            if (System.currentTimeMillis() >= timedOut) {
                return false;
            }
        }
        return true;
    }

    protected Optional<Channel> getChannel(String name) {
        return server.channels().stream().filter(c -> c.name().equals(name)).findFirst();
    }

    protected Optional<User> getUser(String nickname) {
        return server.users().stream().filter(u -> u.info().nickname().equals(nickname)).findFirst();
    }

    @Override
    protected void init() {
        super.init();
        try {
            this.connections = new Connection[]{createConnection()};
        } catch (IOException e) {
            fail("Failed to open client socket", e);
        }
    }

    protected String getRandomString(int length) {
        return getRandomString(length, Charset.defaultCharset());
    }

    protected String getRandomString(int length, Charset charset) {
        return getRandomString(length, i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97), charset);
    }

    protected String getRandomString(int length, IntPredicate filter) {
        return getRandomString(length, filter, Charset.defaultCharset());
    }

    protected String getRandomString(int length, IntPredicate filter, Charset charset) {
        return getRandomString(length, 48, 123, filter, charset);
    }

    protected String getRandomString(int length, int origin, int bound, IntPredicate filter) {
        return getRandomString(length, origin, bound, filter, Charset.defaultCharset());
    }

    protected String getRandomString(int length, int origin, int bound, IntPredicate filter, Charset charset) {
        return new String(new Random().ints(origin, bound)
                                      .filter(filter)
                                      .limit(length)
                                      .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                                               StringBuilder::append)
                                      .toString().getBytes(), charset);
    }

    protected record Connection(long joinedAt, Socket socket, BufferedReader input, PrintStream output) {

        public void createUser(String user, String realName) {
            createUser(user, user, realName);
        }

        public void createUser(String nick, String user, String realName) {
            send("PASS %s".formatted(baseSettings.pass));
            send("NICK %s".formatted(nick));
            send("USER %s 0 * :%s".formatted(user, realName));
            waitFor(() -> awaitMessage()[0] != null);
            ignoreMessage(4 + attrLength + Math.max(1, baseSettings.motd.length));
        }

        public String[] send(String message, int count) {
            send(message);
            return awaitMessage(count);
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
            String[] messages = new String[count];
            StringBuilder line = new StringBuilder();
            InputStream   inputStream;

            for (int i = 0; i < count; i++) {
                try {
                    inputStream = socket.getInputStream();
                    int read;
                    while ((read = inputStream.read()) != ENDING.charAt(0) && read != -1) {
                        line.append((char) read);
                    }
                    if (read == -1) {
                        socket.close();
                        messages[i] = SOCKET_CLOSE[0];
                        continue;
                    }
                    inputStream.skip(ENDING.length() - 1);
                    messages[i] = line.toString();
                } catch (SocketTimeoutException ignored) {
                    messages[i] = EMPTY_ARRAY[0];
                } catch (IOException e) {
                    fail(e);
                } finally {
                    line.setLength(0);
                }
            }
            return messages;
        }

        public void oper(int index) {
            send("OPER %s %s".formatted(baseSettings.operators.get(index).username(), baseSettings.operators.get(index).password()));
            assumeTrue(waitFor(() -> awaitMessage()[0].contains("381")));
        }

        public boolean testConnection(int port) {
            try (Socket ignored = new Socket("localhost", port)) {
                return true;
            } catch (ConnectException e) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                return false;
            } catch (IOException e) {
                fail(e);
                return false;
            }
        }
    }
}

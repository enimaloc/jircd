/*
 * ServerTest
 *
 * 0.0.1
 *
 * 05/08/2022
 */
package fr.enimaloc.jircd;

import fr.enimaloc.enutils.classes.NumberUtils;
import fr.enimaloc.jircd.server.JIRCD;
import fr.enimaloc.jircd.server.ServerSettings;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.BindException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */
public class ServerBase {
    public static final String ENDING                        = "\r\n";
    public static final int    TIMEOUT_WHEN_WAITING_RESPONSE = NumberUtils.getSafe(
            System.getenv("TIMEOUT_WHEN_WAITING_RESPONSE"), Integer.class).orElse(500);

    public static final String[] EMPTY_ARRAY  = new String[]{"\0"};
    public static final String[] SOCKET_CLOSE = new String[]{null};

    protected static ServerSettings baseSettings;
    protected static int            attrLength;
    protected        JIRCD          server;
    protected static Logger         logger = LoggerFactory.getLogger(ServerBase.class);

    protected ServerSettings.Builder buildSettings() {
        return new ServerSettings.Builder()
                .motd(new String[0])
                .host("jircd-host")
                .networkName("JIRCD")
                .pass("jircd-pass")
                .pingTimeout(TimeUnit.DAYS.toMillis(1))
                .operators(
                        new ServerSettings.Operator("oper", "*", "oper"),
                        new ServerSettings.Operator("googleOper", "google", "pass")
                 );
    }

    protected void init() {
        ServerSettings.Builder builder = buildSettings();

        ServerSettings tmp = builder.build();
        logger.info("Creating server with settings: {}", tmp);
        while (server == null) {
            try {
                server     = new JIRCD(tmp) {
                    @Override
                    public void shutdown() {
                        logger.info("Stopping server...");
                        this.isShutdown = true;
                        this.interrupt();
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            logger.warn("Failed to correctly shutdown server", e);
                        }
                    }
                };
                attrLength = (int) Math.max(Math.ceil(server.supportAttribute().length() / 13.), 1);
                baseSettings = server.settings().copy();
                break;
            } catch (BindException ignored) {
                int newPort = new Random().nextInt(1000) + 1024;
                logger.warn("Port {} is currently used, replaced with {}", tmp.port(), newPort);
                tmp = builder.port(newPort).build();
            } catch (IOException e) {
                fail("Can't start IRCServer", e);
                break;
            }
        }
    }

    protected void setSettings(ServerSettings settings) {
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

    protected void off() {
        server.shutdown();
    }
}

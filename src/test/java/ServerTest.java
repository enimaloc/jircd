import com.github.enimaloc.irc.jircd.api.JIRCD;
import com.github.enimaloc.irc.jircd.api.ServerSettings;
import com.github.enimaloc.irc.jircd.internal.JIRCDImpl;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    public static final long TIME_OUT_BETWEEN_TEST = 100;

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

    @AfterEach
    void tearDown(TestInfo info) throws InterruptedException {
        logger.info("{} test end", info.getDisplayName());
        server.shutdown();
        Thread.sleep(TIME_OUT_BETWEEN_TEST); // Await server shutdown
    }
}
package fr.enimaloc.jircd.commands.user;

import fr.enimaloc.jircd.user.User;
import java.net.SocketException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@CommandClazzTest
class WhoisCommandTest extends UserCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }
//":jircd-host 276 bob <nick> :has client certificate fingerprint <fingerprint>",
//":jircd-host 307 bob <nick> :is a registered nick",
//":jircd-host 311 bob <nick> <username> <host> * :<realname>",
//":jircd-host 312 bob <nick> <server> :<serverinfo>",
//":jircd-host 313 bob <nick> :is an IRC operator",
//":jircd-host 317 bob <nick> <secs> <signon> :seconds idle, signon time",
//":jircd-host 319 bob <nick> :<channels>",
//":jircd-host 320 bob <nick> :<special>",
//":jircd-host 330 bob <nick> <account> :is logged in as",
//":jircd-host 338 bob <nick> <actually>",
//":jircd-host 378 bob <nick> :is connecting from <host>",
//":jircd-host 379 bob <nick> :is using modes <modes>",
//":jircd-host 671 bob <nick> :is using a secure connection",
//":jircd-host 318 bob <nick> :End of /WHOIS list"

    @Test
    void whoisSelfTest() {
        Optional<User> bobOpt = getUser("bob");
        assumeTrue(bobOpt.isPresent());
        User bob = bobOpt.get();

        assertArrayEquals(new String[]{
//                            ":jircd-host 276 bob bob :has client certificate fingerprint <fingerprint>",
//                            ":jircd-host 307 bob bob :is a registered nick",
                ":jircd-host 311 bob bob bob 127.0.0.1 * :Mobbie Plav",
                ":jircd-host 312 bob bob jircd-host :jircd is a lightweight IRC server written in Java.",
//                            ":jircd-host 313 bob bob :is an IRC operator",
                ":jircd-host 317 bob bob 0 " + bob.info().joinedAt() + " :seconds idle, signon time",
                ":jircd-host 319 bob bob :",
//                            ":jircd-host 320 bob bob :<special>",
//                            ":jircd-host 330 bob bob <account> :is logged in as",
//                            ":jircd-host 338 bob bob <actually>",
                ":jircd-host 378 bob bob :is connecting from 127.0.0.1",
                ":jircd-host 379 bob bob :is using modes +",
                ":jircd-host 671 bob bob :is using a secure connection",
                ":jircd-host 318 bob bob :End of /WHOIS list"
        }, connections[0].send("WHOIS bob", 8));
    }

    @Test
    void whoisJohnTest() {
        Optional<User> johnOpt = getUser("john");
        assumeTrue(johnOpt.isPresent());
        User john = johnOpt.get();

        assertArrayEquals(new String[]{
//                            ":jircd-host 276 bob john :has client certificate fingerprint <fingerprint>",
//                            ":jircd-host 307 bob john :is a registered nick",
                ":jircd-host 311 bob john john enimaloc.fr * :John Doe",
                ":jircd-host 312 bob john jircd-host :jircd is a lightweight IRC server written in Java.",
//                            ":jircd-host 313 bob john :is an IRC operator",
                ":jircd-host 317 bob john " + ((System.currentTimeMillis() - john.lastActivity()) / 1000) + " " + john.info().joinedAt() + " :seconds idle, signon time",
                ":jircd-host 319 bob john :",
//                            ":jircd-host 320 bob john :<special>",
//                            ":jircd-host 330 bob john <account> :is logged in as",
//                            ":jircd-host 338 bob john <actually>",
                ":jircd-host 378 bob john :is connecting from enimaloc.fr",
                ":jircd-host 379 bob john :is using modes +",
//                            ":jircd-host 671 bob john :is using a secure connection",
                ":jircd-host 318 bob john :End of /WHOIS list"
        }, connections[0].send("WHOIS john", 7));
    }

    @Test
    void whoisFredTest() {
        Optional<User> fredOpt = getUser("fred");
        assumeTrue(fredOpt.isPresent());
        User fred = fredOpt.get();

        assertArrayEquals(new String[]{
//                            ":jircd-host 276 bob fred :has client certificate fingerprint <fingerprint>",
//                            ":jircd-host 307 bob fred :is a registered nick",
                ":jircd-host 311 bob fred fred 127.0.0.1 * :Fred Bloggs",
                ":jircd-host 312 bob fred jircd-host :jircd is a lightweight IRC server written in Java.",
                ":jircd-host 313 bob fred :is an IRC operator",
                ":jircd-host 317 bob fred " + ((System.currentTimeMillis() - fred.lastActivity()) / 1000) + " " + fred.info().joinedAt() + " :seconds idle, signon time",
                ":jircd-host 319 bob fred :",
//                            ":jircd-host 320 bob fred :<special>",
//                            ":jircd-host 330 bob fred <account> :is logged in as",
//                            ":jircd-host 338 bob fred <actually>",
                ":jircd-host 378 bob fred :is connecting from 127.0.0.1",
                ":jircd-host 379 bob fred :is using modes +o",
                ":jircd-host 671 bob fred :is using a secure connection",
                ":jircd-host 318 bob fred :End of /WHOIS list"
        }, connections[0].send("WHOIS fred", 9));
    }

    @Test
    void whoisWizTest() {
        try {
            connections[0].socket().setSoTimeout(10000);
        } catch (SocketException e) {
            fail("Failed to increase timeout", e);
        }
        String torIp = "93.95.230.253";

        addConnections(1);
        connections[4].createUser("WiZ", "Jarko Oikarinen");
        Optional<User> wizOpt = getUser("WiZ");
        assumeTrue(wizOpt.isPresent());
        User wiz = wizOpt.get();
        wiz.info().setHost(torIp); // https://check.torproject.org/torbulkexitlist
        wiz.modes().registered(true);
        assumeTrue(wiz.modes().registered());

        assertArrayEquals(new String[]{
//                            ":jircd-host 276 bob WiZ :has client certificate fingerprint <fingerprint>",
                ":jircd-host 307 bob WiZ :is a registered nick",
                ":jircd-host 311 bob WiZ WiZ " + torIp + " * :Jarko Oikarinen",
                ":jircd-host 312 bob WiZ jircd-host :jircd is a lightweight IRC server written in Java.",
//                            ":jircd-host 313 bob WiZ :is an IRC operator",
                ":jircd-host 317 bob WiZ " + ((System.currentTimeMillis() - wiz.lastActivity()) / 1000) + " " + wiz.info().joinedAt() + " :seconds idle, signon time",
                ":jircd-host 319 bob WiZ :",
//                            ":jircd-host 320 bob WiZ :<special>",
//                            ":jircd-host 330 bob WiZ <account> :is logged in as",
//                            ":jircd-host 338 bob WiZ <actually>",
                ":jircd-host 378 bob WiZ :is connecting from " + torIp,
                ":jircd-host 379 bob WiZ :is using modes +r",
                ":jircd-host 671 bob WiZ :is using a secure connection",
                ":jircd-host 318 bob WiZ :End of /WHOIS list"
        }, connections[0].send("WHOIS WiZ", 9));
    }

    @Test
    void whoisJaneTest() {
        Optional<User> janeOpt = getUser("jane");
        assumeTrue(janeOpt.isPresent());
        User jane = janeOpt.get();

        connections[0].send("WHOIS jane");
        assertArrayEquals(new String[]{
                ":jircd-host 301 bob jane :Away",
//                            ":jircd-host 276 bob jane :has client certificate fingerprint <fingerprint>",
//                            ":jircd-host 307 bob jane :is a registered nick",
                ":jircd-host 311 bob jane jane 127.0.0.1 * :Jane Doe",
                ":jircd-host 312 bob jane jircd-host :jircd is a lightweight IRC server written in Java.",
//                            ":jircd-host 313 bob jane :is an IRC operator",
                ":jircd-host 317 bob jane " + ((System.currentTimeMillis() - jane.lastActivity()) / 1000) + " " + jane.info().joinedAt() + " :seconds idle, signon time",
                ":jircd-host 319 bob jane :",
//                            ":jircd-host 320 bob jane :<special>",
//                            ":jircd-host 330 bob jane <account> :is logged in as",
//                            ":jircd-host 338 bob jane <actually>",
                ":jircd-host 378 bob jane :is connecting from 127.0.0.1",
                ":jircd-host 379 bob jane :is using modes +",
                ":jircd-host 671 bob jane :is using a secure connection",
                ":jircd-host 318 bob jane :End of /WHOIS list"
        }, connections[0].awaitMessage(9));
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
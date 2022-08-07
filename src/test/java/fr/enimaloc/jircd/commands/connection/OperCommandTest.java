package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.SocketBase;
import fr.enimaloc.jircd.server.ServerSettings;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class OperCommandTest extends ConnectionCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

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

    @AfterEach
    void tearDown() {
        off();
    }
}
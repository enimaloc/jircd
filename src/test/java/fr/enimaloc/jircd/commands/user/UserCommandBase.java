/*
 * UserCommandBase
 *
 * 0.0.1
 *
 * 06/08/2022
 */
package fr.enimaloc.jircd.commands.user;

import fr.enimaloc.jircd.SocketBase;
import fr.enimaloc.jircd.user.User;
import java.util.Optional;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 *
 */
public class UserCommandBase extends SocketBase {
    @Override
    protected void init() {
        super.init();

        addConnections(3);
        connections[0].createUser("bob", "Mobbie Plav");
        connections[1].createUser("fred", "Fred Bloggs");
        connections[2].createUser("john", "John Doe");
        connections[3].createUser("jane", "Jane Doe");

        connections[1].oper(0);

        Optional<User> johnOpt = server.users()
                                       .stream()
                                       .filter(u -> u.info().nickname().equals("john"))
                                       .findFirst();
        assumeTrue(johnOpt.isPresent());
        User john = johnOpt.get();
        john.info().setHost("enimaloc.fr");

        Optional<User> janeOpt = server.users()
                                       .stream()
                                       .filter(u -> u.info().nickname().equals("jane"))
                                       .findFirst();
        assumeTrue(janeOpt.isPresent());
        User jane = janeOpt.get();
        jane.away("Away");
    }
}

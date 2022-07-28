/*
 * RestartCommand
 *
 * 0.0.1
 *
 * 29/07/2022
 */
package fr.enimaloc.jircd.commands.operator;

import fr.enimaloc.jircd.Main;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.server.JIRCD;
import fr.enimaloc.jircd.user.User;

/**
 *
 */
@Command(name = "restart")
public class RestartCommand {

    @Command
    public void execute(User user) {
        if (!user.modes().oper()) {
            user.send(Message.ERR_NOPRIVILEGES.client(user.info()));
            return;
        }
        user.server().shutdown();
        JIRCD.newInstance();
    }
}

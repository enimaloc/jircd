/*
 * SQuitCommand
 *
 * 0.0.1
 *
 * 29/07/2022
 */
package fr.enimaloc.jircd.commands.operator;

import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;

/**
 *
 */
@Command(name = "squit")
public class SQuitCommand {

    // TODO: 29/07/2022 - Implement connection to server before
    @Command(trailing = true)
    public void execute(User user, String server, String message) {
        if (!user.modes().oper()) {
            user.send(Message.ERR_NOPRIVILEGES.client(user.info()));
            return;
        }
        user.send(Message.ERR_NOSUCHSERVER.client(user.info()).addFormat("server name", server));
    }
}

/*
 * WhowasCommand
 *
 * 0.0.1
 *
 * 19/07/2022
 */
package fr.enimaloc.jircd.commands.user;

import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;

/**
 *
 */
@Command(name = "whowas")
public class WhowasCommand {

    @Command
    public void execute(User user, String nick) {
        execute(user, nick, "0");
    }

    // todo: 26/07/2022 implement required structure(name history) for command
    @Command
    public void execute(User user, String nick, String count) {
        user.send(Message.ERR_WASNOSUCHNICK.client(user.info()));
        user.send(Message.RPL_ENDOFWHOWAS.client(user.info())
                                         .addFormat("nick", nick));
    }
}

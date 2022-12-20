/*
 * HelpCommand
 *
 * 0.0.1
 *
 * 19/07/2022
 */
package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;

/**
 *
 */
@Command(name = "help")
public class HelpCommand {

    @Command
    public void execute(User user) {
        user.send(Message.ERR_HELPNOTFOUND.client(user.info())
                                          .addFormat("subject", "\0")
                                          .format(user.server().settings().host())
                                          .replace("\0 ", ""));
    }

    @Command
    public void execute(User user, String subject) {
        user.send(Message.ERR_HELPNOTFOUND.client(user.info()).addFormat("subject", subject));
    }

}

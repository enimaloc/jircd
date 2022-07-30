/*
 * LinkCommand
 *
 * 0.0.1
 *
 * 29/07/2022
 */
package fr.enimaloc.jircd.commands.optional;

import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;

/**
 *
 */
@Command(name = "links")
public class LinksCommand {

    @Command
    public void execute(User user) {
        user.send(Message.RPL_LINKS.client(user.info())
                                   .addFormat("mask", "*")
                                   .addFormat("server", user.server().settings().host)
                                   .addFormat("hopcount", "0")
                                   .addFormat("serverinfo", user.server().settings().description));
        user.send(Message.RPL_ENDOFLINKS.client(user.info())
                                        .addFormat("mask", "*"));
    }
}

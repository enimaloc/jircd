/*
 * LUserCommand
 *
 * 0.0.1
 *
 * 19/07/2022
 */
package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;
import fr.enimaloc.jircd.user.UserState;

/**
 *
 */
@Command(name = "luser")
public class LUserCommand {

    @Command
    public void execute(User user) {
        user.send(Message.RPL_LUSERCLIENT.client(user.info())
                                         .addFormat("u", user.server()
                                                             .users()
                                                             .stream()
                                                             .filter(u -> u.state() == UserState.LOGGED)
                                                             .filter(u -> !u.modes().invisible())
                                                             .count())
                                         .addFormat("i", user.server()
                                                             .users()
                                                             .stream()
                                                             .filter(u -> u.state() == UserState.LOGGED)
                                                             .filter(u -> u.modes().invisible())
                                                             .count())
                                         .addFormat("s", 1));
        user.send(Message.RPL_LUSEROP.client(user.info())
                                     .addFormat("ops", user.server()
                                                           .users()
                                                           .stream()
                                                           .filter(u -> u.state() == UserState.LOGGED)
                                                           .filter(u -> u.modes().oper())
                                                           .count()));
        user.send(Message.RPL_LUSERUNKNOWN.client(user.info())
                                          .addFormat("connections", user.server()
                                                                        .users()
                                                                        .stream()
                                                                        .filter(u -> u.state() ==
                                                                                     UserState.REGISTRATION)
                                                                        .count()));
        user.send(Message.RPL_LUSERCHANNELS.client(user.info())
                                           .addFormat("channels", user.server()
                                                                      .channels()
                                                                      .size()));
        user.send(Message.RPL_LUSERME.client(user.info())
                                     .addFormat("u", user.server().users().size())
                                     .addFormat("s", 1));
    }

}

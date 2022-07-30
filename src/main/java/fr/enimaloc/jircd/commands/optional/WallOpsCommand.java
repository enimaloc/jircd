/*
 * WallOpsCommand
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
@Command(name = "wallops")
public class WallOpsCommand {

    @Command(trailing = true)
    public void execute(User user, String message) {
        if (!user.modes().oper()) {
            user.send(Message.ERR_NOPRIVILEGES);
            return;
        }
        user.server().broadcast(user.info().format(),
                                Message.CMD_WALLOPS.rawFormat(message),
                                u -> u.modes().wallops() || u.equals(user));
    }

}

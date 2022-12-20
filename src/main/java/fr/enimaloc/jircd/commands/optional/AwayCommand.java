/*
 * AwayCommand
 *
 * 0.0.1
 *
 * 29/07/2022
 */
package fr.enimaloc.jircd.commands.optional;

import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;
import java.util.function.Predicate;

/**
 *
 */
@Command(name = "away")
public class AwayCommand {
    public static final Predicate<User> AWAY_CAP = u -> u.info().capabilities().contains("away-notify");

    @Command
    public void execute(User user) {
        user.channels()
            .forEach(channel -> channel.broadcast(user.info().format(), Message.CMD_AWAY, AWAY_CAP));
        user.away(null);
        user.send(Message.RPL_UNAWAY.client(user.info()));
    }

    @Command(trailing = true)
    public void execute(User user, String message) {
        user.channels()
            .forEach(channel -> channel.broadcast(user.info().format(), Message.CMD_AWAY.trailing(message), AWAY_CAP));
        user.away(message);
        user.send(Message.RPL_NOWAWAY.client(user.info()));
    }

}

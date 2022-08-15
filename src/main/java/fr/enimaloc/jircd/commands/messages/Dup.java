/*
 * Dup
 *
 * 0.0.1
 *
 * 15/08/2022
 */
package fr.enimaloc.jircd.commands.messages;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.message.Mask;
import fr.enimaloc.jircd.message.Regex;
import fr.enimaloc.jircd.user.User;
import fr.enimaloc.jircd.utils.function.TriConsumer;

/**
 *
 */
public class Dup {

    Dup() {
    }

    public static void dup1(
            User user,
            String targets,
            String trailing,
            TriConsumer<User, String, String> channelFun,
            TriConsumer<User, String, String> userFun
    ) {
        for (String target : targets.split(",")) {
            if (target.isEmpty()) {
                continue;
            }
            if (Regex.CHANNEL.matcher(target.replaceFirst(Regex.CHANNEL_PREFIX.pattern(), "")).matches()) {
                channelFun.accept(user, target, trailing);
            } else if (Regex.NICKNAME.matcher(target).matches()) {
                userFun.accept(user, target, trailing);
            }
        }
    }

    public static boolean restrained(Channel channel, User user) {
        return (channel.modes().bans().stream().anyMatch(mask -> new Mask(mask).toPattern()
                                                                               .matcher(user.info().full())
                                                                               .matches()) &&
                channel.modes().except().stream().noneMatch(mask -> new Mask(mask).toPattern()
                                                                                  .matcher(user.info().full())
                                                                                  .matches())) ||
               (channel.modes().noExternalMessage() && !channel.users().contains(user)) ||
               (channel.modes().moderate() && !Regex.CHANNEL_PREFIX.matcher(
                       channel.prefix(user) + user.modes().prefix()).matches());
    }
}

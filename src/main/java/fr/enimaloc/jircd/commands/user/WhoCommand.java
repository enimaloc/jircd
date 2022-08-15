/*
 * WhoCommand
 *
 * 0.0.1
 *
 * 19/07/2022
 */
package fr.enimaloc.jircd.commands.user;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Mask;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.server.JIRCD;
import fr.enimaloc.jircd.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 */
@Command(name = "who")
public class WhoCommand {

    @Command
    public void execute(User user, String mask) {
        JIRCD server = user.server();
        Optional<User> userOpt = server.users()
                                       .stream()
                                       .filter(u -> mask.equals(u.info().nickname()))
                                       .findFirst();
        Optional<Channel> channelOpt = server.channels()
                                             .stream()
                                             .filter(c -> c.name().equals(mask))
                                             .findFirst();

        List<User> targets =
                channelOpt.map(Channel::users)
                          .map(ArrayList::new)
                          .orElse(userOpt.map(List::of)
                                         .map(ArrayList::new)
                                         .orElse(new ArrayList<>(server.users()
                                                                       .stream()
                                                                       .filter(u -> u.info()
                                                                                     .nickname()
                                                                                     .matches(new Mask(
                                                                                             mask).toRegex()))
                                                                       .toList())));

        for (User target : targets) {
            user.send(Message.RPL_WHOREPLY.client(user.info())
                                          .channel(target.channels()
                                                         .stream()
                                                         .findFirst()
                                                         .map(Channel::name)
                                                         .orElse("*"))
                                          .addFormat("username", target.info().username())
                                          .addFormat("host", target.info().host().replaceFirst(":", "0:"))
                                          .addFormat("server", server.settings().host())
                                          .addFormat("nick", target.info().nickname())
                                          .addFormat("flags", target.away()
                                                                    .map(away -> "G")
                                                                    .orElse("H")
                                                              + (target.modes().oper() ? "*" : "")
                                                              + (target.channels()
                                                                       .stream()
                                                                       .findFirst()
                                                                       .map(channel -> channel.prefix(target))
                                                                       .orElse("")))
                                          .addFormat("hopcount", 0)
                                          .addFormat("realname", target.info().realName()));

        }

        user.send(Message.RPL_ENDOFWHO.client(user.info()).addFormat("mask", mask));
    }

}

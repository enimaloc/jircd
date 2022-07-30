/*
 * WhoisCommand
 *
 * 0.0.1
 *
 * 19/07/2022
 */
package fr.enimaloc.jircd.commands.user;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;
import java.util.stream.Collectors;

/**
 *
 */
@Command(name = "whois")
public class WhoisCommand {

    @Command
    public void execute(User user, String nick) {
        User target = user.server().users()
                          .stream()
                          .filter(u -> u.info().nickname().equals(nick))
                          .findFirst()
                          .orElse(null);
        if (target == null) {
            user.send(Message.ERR_NOSUCHNICK.client(user.info())
                                            .addFormat("nickname", nick));
            return;
        }
        target.away()
              .map(away -> Message.RPL_AWAY.addFormat("message", away))
              .map(msg -> msg.addFormat("nick", nick))
              .map(msg -> msg.client(user.info()))
              .ifPresent(user::send);
//         TODO: 23/07/2022 implement certificate tls before
//        if (target.modes().oper() || target.equals(user)) {
//            user.send(Message.RPL_WHOISCERTFP.client(user.info())
//                                             .addFormat("nick", target.info().nickname())
//                                             .addFormat("fingerprint", target.info().fingerprint()));
//
//        }

        if (target.modes().registered()) {
            user.send(Message.RPL_WHOISREGNICK.client(user.info())
                                            .addFormat("nick", target.info().nickname()));
        }
        user.send(Message.RPL_WHOISUSER.client(user.info())
                                       .addFormat("nick", target.info().nickname())
                                       .addFormat("username", target.info().username())
                                       .addFormat("host", target.info().host().replaceFirst(":", "0:"))
                                       .addFormat("realname", target.info().realName()));
        user.send(Message.RPL_WHOISSERVER.client(user.info())
                                         .addFormat("nick", target.info().nickname())
                                         .addFormat("server", target.server().settings().host)
                                         .addFormat("serverinfo", target.server().settings().description));
        if (target.modes().oper()) {
            user.send(Message.RPL_WHOISOPERATOR.client(user.info())
                                               .addFormat("nick", target.info().nickname()));
        }
        user.send(Message.RPL_WHOISIDLE.client(user.info())
                                       .addFormat("nick", target.info().nickname())
                                       .addFormat("secs", (System.currentTimeMillis() - target.lastActivity()) / 1000)
                                       .addFormat("signon", target.info().joinedAt()));
        user.send(Message.RPL_WHOISCHANNELS.client(user.info())
                                           .addFormat("nick", target.info().nickname())
                                           .addFormat("channels", target.channels().stream()
                                                                        .map(Channel::name)
                                                                        .collect(Collectors.joining(" "))));
//         TODO: 20/07/2022 What's that?
//        user.send(Message.RPL_WHOISSPECIAL.client(user.info())
//                                          .addFormat("nick", target.info().nickname())
//                                          .addFormat("special", ""));
//         TODO: 20/07/2022 What's condition for reply this message?
//        user.send(Message.RPL_WHOISACCOUNT.client(user.info())
//                                        .addFormat("nick", target.info().nickname())
//                                        .addFormat("account", ""));
//         TODO: 20/07/2022 See todo of RPL_WHOISACTUALLY
//        user.send(Message.RPL_WHOISACTUALLY.client(user.info())
//                                           .addFormat("nick", target.info().nickname())
//                                           .addFormat("actually", ""));
        user.send(Message.RPL_WHOISHOST.client(user.info())
                                       .addFormat("nick", target.info().nickname())
                                       .addFormat("host", target.info().host()));
        user.send(Message.RPL_WHOISMODES.client(user.info())
                                        .addFormat("nick", target.info().nickname())
                                        .addFormat("modes", "+" + target.modes().toString()));
        if (target.info().secure()) {
            user.send(Message.RPL_WHOISSECURE.client(user.info())
                                             .addFormat("nick", target.info().nickname()));
        }


        user.send(Message.RPL_ENDOFWHOIS.client(user.info())
                                        .addFormat("nick", target.info().nickname()));
    }

    // TODO: 20/07/2022 - need to understand how to implement this command and specially target parameter, this command return ENDOFWHOIS for the moment
    @Command
    public void execute(User user, String target, String nick) {
        user.send(Message.RPL_ENDOFWHOIS.client(user.info())
                                        .addFormat("nick", nick));
    }
}

package com.github.enimaloc.irc.jircd.internal.commands.optional;

import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import java.util.Optional;

@Command(name = "userhost")
public class UserhostCommand {

    @Command
    public void execute(User user, String a) {
        execute(user, a, null);
    }

    @Command
    public void execute(User user, String a, String b) {
        execute(user, a, b, null);
    }

    @Command
    public void execute(User user, String a, String b, String c) {
        execute(user, a, b, c, null);
    }

    @Command
    public void execute(User user, String a, String b, String c, String d) {
        execute(user, a, b, c, d, null);
    }

    @Command
    public void execute(User user, String a, String b, String c, String d, String e) {
        StringBuilder ret = new StringBuilder();
        for (String nickname : new String[]{a, b, c, d, e}) {
            if (nickname == null) {
                continue;
            }
            Optional<User> userOpt = user.server()
                                         .users()
                                         .stream()
                                         .filter(u -> u.info().format().equals(nickname))
                                         .findFirst();
            if (userOpt.isEmpty()) {
                continue;
            }
            User u = userOpt.get();
            ret.append(u.info().nickname())
               .append(u.modes().oper() ? "*" : "")
               .append(u.away().isPresent() ? "-" : "+")
               .append(u.info().host())
               .append(" ");
        }
        user.send(Message.RPL_USERHOST.parameters(user.info().format(), ret.toString().trim()));
    }

}

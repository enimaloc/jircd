package fr.enimaloc.jircd.commands.optional;

import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;
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
        user.send(Message.RPL_USERHOST.client(user.info()).addFormat("reply", ret.toString().trim()));
    }

}

package com.github.enimaloc.irc.jircd.commands.miscellaneous;

import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;
import java.util.Optional;

@Command(name = "kill", trailing = true)
public class KillCommand {

    @Command
    public void execute(User user, String nickname, String reason) {
        if (!user.modes().oper() && !user.modes().localOper()) {
            user.send(Message.ERR_NOPRIVILEGES.client(user.info()));
            return;
        }
        Optional<User> userOpt = user.server()
                                     .users()
                                     .stream()
                                     .filter(u -> u.info().format().equals(nickname))
                                     .findFirst();
        if (userOpt.isEmpty()) {
            return;
        }
        User   userObj    = userOpt.get();
        String quitReason = "Killed (%s (%s))".formatted(user.info().format(), reason);
        userObj.send("Closing Link: " + userObj.server().settings().host + " (" + quitReason + ")");
        userObj.terminate(quitReason);
    }

}

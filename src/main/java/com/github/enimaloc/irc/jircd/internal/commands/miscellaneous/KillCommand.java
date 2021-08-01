package com.github.enimaloc.irc.jircd.internal.commands.miscellaneous;

import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import java.util.Optional;

@Command(name = "kill", trailing = true)
public class KillCommand {

    @Command
    public void execute(User user, String nickname, String reason) {
        if (!user.modes().oper() && !user.modes().localOper()) {
            user.send(Message.ERR_NOPRIVILEGES.parameters(user.info().format()));
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

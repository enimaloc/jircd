package com.github.enimaloc.irc.jircd.internal.commands.connection;

import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.Regex;
import com.github.enimaloc.irc.jircd.internal.UserImpl;
import com.github.enimaloc.irc.jircd.internal.UserState;
import com.github.enimaloc.irc.jircd.internal.commands.Command;

@Command(name = "nick")
public class NickCommand {

    @Command
    public void execute(User user, String nickname) {
        if (!user.info().passwordValid()) {
            return;
        }
        if (!nickname.matches(Regex.NICKNAME.pattern())) {
            user.send(Message.ERR_ERRONEUSNICKNAME.parameters(user.info().format(), nickname));
            return;
        }
        if (user.server().users().stream().anyMatch(
                u -> u.info().nickname() != null && u.info().nickname().equals(nickname))) {
            user.send(Message.ERR_NICKNAMEINUSE.parameters(user.info().format(), nickname));
            return;
        }
        if (user.state() == UserState.LOGGED) {
            user.server().broadcast(user.info().format() + " NICK " + nickname);
        }
        user.info().setNickname(nickname);
        if (user.info().canRegistrationBeComplete()) {
            ((UserImpl) user).finishRegistration();
        }
    }
}

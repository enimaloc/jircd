package com.github.enimaloc.irc.jircd.internal.commands.connection;

import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.Regex;
import com.github.enimaloc.irc.jircd.internal.UserImpl;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import com.github.enimaloc.irc.jircd.internal.exception.IRCException;

@Command(name = "nick")
public class NickCommand {

    @Command
    public void execute(User user, String nickname) {
        if (!user.info().passwordValid()) {
            return;
        }
        if (!nickname.matches(Regex.NICKNAME.pattern())) {
            throw new IRCException.ErroneusNickname(user.server().settings(), user.info(), nickname);
        }
        if (user.server().users().stream().anyMatch(
                u -> u.info().nickname() != null && u.info().nickname().equals(nickname))) {
            throw new IRCException.NicknameInUse(user.server().settings(), user.info(), nickname);
        }
        user.server().broadcast(user.info().format() + " NICK " + nickname);
        user.info().setNickname(nickname);
        if (user.info().isRegistrationComplete()) {
            ((UserImpl) user).finishRegistration();
        }
    }
}

package com.github.enimaloc.irc.jircd.internal.commands.connection;

import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.UserImpl;
import com.github.enimaloc.irc.jircd.internal.UserState;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import com.github.enimaloc.irc.jircd.internal.exception.IRCException;

@Command(name = "user")
public class UserCommand {

    @Command(trailing = true)
    public void execute(User user, String username, String __, String ___, String realName) {
        if (!user.info().passwordValid()) {
            return;
        }
        if (user.state() == UserState.LOGGED) {
            throw new IRCException.AlreadyRegisteredError(user.server().settings(), user.info());
        }
        user.info().setUsername(username);
        user.info().setRealName(realName);
        if (user.info().isRegistrationComplete()) {
            ((UserImpl) user).finishRegistration();
        }
    }
}

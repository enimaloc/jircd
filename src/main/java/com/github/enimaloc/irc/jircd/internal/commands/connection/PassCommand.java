package com.github.enimaloc.irc.jircd.internal.commands.connection;

import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.UserImpl;
import com.github.enimaloc.irc.jircd.internal.UserState;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import com.github.enimaloc.irc.jircd.internal.exception.IRCException;

@Command(name = "Pass")
public class PassCommand {

    @Command
    public void execute(User user, String password) {
        if (!user.server().settings().pass.equals(password)) {
            throw new IRCException.PasswdMismatch(user.server().settings(), user.info());
        }
        if (user.state() == UserState.LOGGED) {
            throw new IRCException.AlreadyRegisteredError(user.server().settings(), user.info());
        }
        ((UserImpl) user).setState(UserState.CONNECTED);
        user.info().validPass();
        if (user.info().isRegistrationComplete()) {
            ((UserImpl) user).finishRegistration();
        }
    }
}

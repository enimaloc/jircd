package com.github.enimaloc.irc.jircd.internal.commands.connection;

import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.UserImpl;
import com.github.enimaloc.irc.jircd.internal.UserState;
import com.github.enimaloc.irc.jircd.internal.commands.Command;

@Command(name = "Pass")
public class PassCommand {

    @Command
    public void execute(User user, String password) {
        if (!user.server().settings().pass.equals(password)) {
            user.send(Message.ERR_PASSWDMISMATCH.parameters(user.info().format()));
            return;
        }
        if (user.state() == UserState.LOGGED) {
            user.send(Message.ERR_ALREADYREGISTERED.parameters(user.info().format()));
            return;
        }
        ((UserImpl) user).state(UserState.CONNECTED);
        user.info().validPass();
        if (user.info().canRegistrationBeComplete()) {
            ((UserImpl) user).finishRegistration();
        }
    }
}

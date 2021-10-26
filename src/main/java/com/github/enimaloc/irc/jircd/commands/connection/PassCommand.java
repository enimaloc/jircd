package com.github.enimaloc.irc.jircd.commands.connection;

import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;
import com.github.enimaloc.irc.jircd.user.UserState;

@Command(name = "Pass")
public class PassCommand {

    @Command
    public void execute(User user, String password) {
        if (!user.server().settings().pass.equals(password)) {
            user.send(Message.ERR_PASSWDMISMATCH.client(user.info()));
            return;
        }
        if (user.state() == UserState.LOGGED) {
            user.send(Message.ERR_ALREADYREGISTERED.client(user.info()));
            return;
        }
        user.state(UserState.CONNECTED);
        user.info().validPass();
        if (user.info().canRegistrationBeComplete()) {
            user.finishRegistration();
        }
    }
}

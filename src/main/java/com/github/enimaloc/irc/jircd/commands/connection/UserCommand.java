package com.github.enimaloc.irc.jircd.commands.connection;

import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;
import com.github.enimaloc.irc.jircd.user.UserState;

@Command(name = "user")
public class UserCommand {

    @Command(trailing = true)
    public void execute(User user, String username, String __, String ___, String realName) {
        if (!user.info().passwordValid()) {
            return;
        }
        if (user.state() == UserState.LOGGED) {
            user.send(Message.ERR_ALREADYREGISTERED.client(user.info()));
            return;
        }
        user.info().setUsername(username);
        user.info().setRealName(realName);
        if (user.info().canRegistrationBeComplete()) {
            user.finishRegistration();
        }
    }
}

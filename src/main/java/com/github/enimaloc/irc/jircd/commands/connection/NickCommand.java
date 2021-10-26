package com.github.enimaloc.irc.jircd.commands.connection;

import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.message.Regex;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.server.ServerSettings;
import com.github.enimaloc.irc.jircd.user.User;
import com.github.enimaloc.irc.jircd.user.UserState;

@Command(name = "nick")
public class NickCommand {

    @Command
    public void execute(User user, String nickname) {
        if (!user.info().passwordValid()) {
            return;
        }
        ServerSettings settings = user.server().settings();
        if (!nickname.matches(Regex.NICKNAME.pattern()) ||
            (settings.unsafeNickname.contains(nickname) && !settings.safeNet.contains(user.info().host()))) {
            user.send(Message.ERR_ERRONEUSNICKNAME.client(user.info()).addFormat("nick", nickname));
            return;
        }
        if (user.server().users().stream().anyMatch(
                u -> u.info().nickname() != null && u.info().nickname().equals(nickname))) {
            user.send(Message.ERR_NICKNAMEINUSE.client(user.info()).addFormat("nick", nickname));
            return;
        }
        if (user.state() == UserState.LOGGED) {
            user.server().broadcast(user.info().format() + " NICK " + nickname);
        }
        user.info().setNickname(nickname);
        if (user.info().canRegistrationBeComplete()) {
            user.finishRegistration();
        }
    }
}

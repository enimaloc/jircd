package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;
import fr.enimaloc.jircd.user.UserState;

@Command(name = "user")
public class UserCommand {

    @Command
    public void executeA(User user, String username, String unused0, String unused1, String realName) {
        execute(user, username, unused0, unused1, realName);
    }

    @Command(trailing = true)
    public void execute(User user, String username, String unused0, String unused1, String realName) {
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

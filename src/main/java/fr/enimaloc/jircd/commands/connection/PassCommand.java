package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;
import fr.enimaloc.jircd.user.UserState;

@Command(name = "Pass")
public class PassCommand {

    @Command
    public void execute(User user, String password) {
        if (user.state() == UserState.LOGGED) {
            user.send(Message.ERR_ALREADYREGISTERED.client(user.info()));
            return;
        }
        if (!user.server().settings().pass().orElse("").equals(password)) {
            user.send(Message.ERR_PASSWDMISMATCH.client(user.info()));
            return;
        }
        user.state(UserState.CONNECTED);
        user.info().validPass();
        if (user.info().canRegistrationBeComplete()) {
            user.finishRegistration();
        }
    }
}

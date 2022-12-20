package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;

@Command(name = "quit")
public class QuitCommand {

    @Command
    public void execute(User user) {
        user.terminate("");
    }

    @Command(trailing = true)
    public void execute(User user, String reason) {
        user.terminate(reason);
    }

}

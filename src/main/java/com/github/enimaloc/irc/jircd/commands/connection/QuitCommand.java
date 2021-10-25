package com.github.enimaloc.irc.jircd.commands.connection;

import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;

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

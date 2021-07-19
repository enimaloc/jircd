package com.github.enimaloc.irc.jircd.internal.commands.connection;

import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;

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

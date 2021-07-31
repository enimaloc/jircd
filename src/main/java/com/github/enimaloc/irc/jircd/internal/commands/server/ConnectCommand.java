package com.github.enimaloc.irc.jircd.internal.commands.server;

import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;

@Command(name = "connect")
public class ConnectCommand {

    @Command
    public void execute(User user, String target) {
        execute(user, target, "");
    }

    @Command
    public void execute(User user, String target, String port) {
        execute(user, target, port, "");
    }

    @Command
    public void execute(User user, String target, String port, String remote) {
        user.send(Message.ERR_UNKNOWNERROR.parameters(user.info().format(), "CONNECT", "Not supported yet"));
    }
}

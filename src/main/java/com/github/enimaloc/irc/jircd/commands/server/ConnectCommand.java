package com.github.enimaloc.irc.jircd.commands.server;

import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;

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

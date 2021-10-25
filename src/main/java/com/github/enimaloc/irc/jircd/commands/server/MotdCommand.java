package com.github.enimaloc.irc.jircd.commands.server;

import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;

@Command(name = "motd")
public class MotdCommand {

    @Command
    public void execute(User user) {
        execute(user, user.server().settings().host);
    }

    @Command
    public void execute(User user, String server) {
        if (!user.server().settings().host.equals(server)) {
            user.send(Message.ERR_NOSUCHSERVER.parameters(user.info().format(), server));
            return;
        }
        String[] motd = user.server().settings().motd;
        if (motd.length == 0) {
            user.send(Message.ERR_NOMOTD.parameters(user.info().format()));
            return;
        }
        user.send(Message.RPL_MOTDSTART.parameters(user.info().format(), server));
        for (String line : motd) {
            user.send(Message.RPL_MOTD.parameters(user.info().format(), line));
        }
        user.send(Message.RPL_ENDOFMOTD.parameters(user.info().format()));
    }
}

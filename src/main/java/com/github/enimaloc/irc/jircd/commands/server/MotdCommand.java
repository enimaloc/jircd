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
            user.send(Message.ERR_NOSUCHSERVER.client(user.info()).addFormat("", server));
            return;
        }
        String[] motd = user.server().settings().motd;
        if (motd.length == 0) {
            user.send(Message.ERR_NOMOTD.client(user.info()));
            return;
        }
        user.send(Message.RPL_MOTDSTART.client(user.info()).addFormat("server", server));
        for (String line : motd) {
            user.send(Message.RPL_MOTD.client(user.info()).addFormat("line of the motd", line));
        }
        user.send(Message.RPL_ENDOFMOTD.client(user.info()));
    }
}

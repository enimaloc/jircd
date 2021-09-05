package com.github.enimaloc.irc.jircd.internal.commands.undocumented.miscellaneous;

import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;

@Command(name = "ping")
public class PingCommand {

    @Command
    public void execute(User user, String server1) {
        user.send("PONG "+server1);
    }

    @Command
    public void execute(User user, String server1, String server2) {
        if (!user.server().settings().host.equals(server2)) {
            user.send(Message.ERR_NOSUCHSERVER.parameters(user.info().format(), server2));
            return;
        }
        execute(user, server1);
    }

    @Command(trailing = true)
    public void executeTrailing(User user, String trailing) {
        execute(user, trailing);
    }
}

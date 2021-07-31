package com.github.enimaloc.irc.jircd.internal.commands.server;

import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Command(name = "time")
public class TimeCommand {

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
        user.send(Message.RPL_TIME.parameters(server,
                                              DateTimeFormatter.ofPattern("EEEE LLLL dd yyyy - HH:mm O", Locale.ENGLISH)
                                                               .format(ZonedDateTime.now())));
    }
}

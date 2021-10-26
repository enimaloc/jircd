package com.github.enimaloc.irc.jircd.commands.server;

import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;
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
            user.send(Message.ERR_NOSUCHSERVER.client(user.info()).addFormat("server name", server));
            return;
        }
        user.send(Message.RPL_TIME.addFormat("server", server)
                                  .addFormat("string showing server's local time",
                                              DateTimeFormatter.ofPattern("EEEE LLLL dd yyyy - HH:mm O", Locale.ENGLISH)
                                                               .format(ZonedDateTime.now())));
    }
}

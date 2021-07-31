package com.github.enimaloc.irc.jircd.internal.commands.server;

import com.github.enimaloc.irc.jircd.api.JIRCD;
import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Command(name = "stats")
public class StatsCommand {

    @Command
    public void execute(User user, String query) {
        execute(user, query, user.server().settings().host);
    }

    @Command
    public void execute(User user, String queryS, String server) {
        JIRCD serverObj = user.server();
        if (!user.server().settings().host.equals(server)) {
            user.send(Message.ERR_NOSUCHSERVER.parameters(user.info().format(), server));
            return;
        }
        switch (Character.toLowerCase(queryS.toCharArray()[0])) {
            case 'c' -> user.send(Message.RPL_STATSCLINE.parameters(user.info().format()));
            case 'h' -> user.send(Message.RPL_STATSHLINE.parameters(user.info().format()));
            case 'i' -> user.send(Message.RPL_STATSILINE.parameters(user.info().format()));
            case 'k' -> user.send(Message.RPL_STATSKLINE.parameters(user.info().format()));
            case 'l' -> user.send(Message.RPL_STATSLLINE.parameters(user.info().format()));
            case 'm' -> serverObj.commandUsage().forEach((command, usage) -> user.send(
                    Message.RPL_STATSCOMMANDS.parameters(command, usage)));
            case 'o' -> user.send(Message.RPL_STATSOLINE.parameters(user.info().format()));
            case 'u' -> user.send(Message.RPL_STATSUPTIME.parameters(
                    ChronoUnit.DAYS.between(serverObj.createdAt().toInstant(), ZonedDateTime.now()),
                    ChronoUnit.HOURS.between(serverObj.createdAt().toInstant(), ZonedDateTime.now()),
                    ChronoUnit.MINUTES.between(serverObj.createdAt().toInstant(), ZonedDateTime.now()),
                    ChronoUnit.SECONDS.between(serverObj.createdAt().toInstant(), ZonedDateTime.now())
            ));
            case 'y' -> user.send(Message.RPL_STATSLINKLINE.parameters(user.info().format()));
        }
        user.send(Message.RPL_ENDOFSTATS.parameters(Character.toUpperCase(queryS.toCharArray()[0])));
    }

}

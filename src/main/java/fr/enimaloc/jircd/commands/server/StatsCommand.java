package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.server.JIRCD;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;
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
            user.send(Message.ERR_NOSUCHSERVER.client(user.info()).addFormat("", server));
            return;
        }
        switch (Character.toLowerCase(queryS.toCharArray()[0])) {
            case 'c' -> user.send(Message.RPL_STATSCLINE.client(user.info()));
            case 'h' -> user.send(Message.RPL_STATSHLINE.client(user.info()));
            case 'i' -> user.send(Message.RPL_STATSILINE.client(user.info()));
            case 'k' -> user.send(Message.RPL_STATSKLINE.client(user.info()));
            case 'l' -> user.send(Message.RPL_STATSLLINE.client(user.info()));
            case 'm' -> serverObj.commandUsage()
                                 .forEach((command, usage) -> user.send(
                                         Message.RPL_STATSCOMMANDS.addFormat("command", command)
                                                                  .addFormat("count", usage)
                                 ));
            case 'o' -> user.send(Message.RPL_STATSOLINE.client(user.info()));
            case 'u' -> user.send(Message.RPL_STATSUPTIME.rawFormat(
                    ChronoUnit.DAYS.between(serverObj.createdAt().toInstant(), ZonedDateTime.now()),
                    ChronoUnit.HOURS.between(serverObj.createdAt().toInstant(), ZonedDateTime.now())%24,
                    ChronoUnit.MINUTES.between(serverObj.createdAt().toInstant(), ZonedDateTime.now())%60,
                    ChronoUnit.SECONDS.between(serverObj.createdAt().toInstant(), ZonedDateTime.now())%60
            ));
            case 'y' -> user.send(Message.RPL_STATSLINKLINE.client(user.info()));
        }
        user.send(Message.RPL_ENDOFSTATS.addFormat("stats letter", Character.toUpperCase(queryS.toCharArray()[0])));
    }

}

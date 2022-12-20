package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;

@Command(name = "ping")
public class PingCommand {

    @Command
    public void execute(User user, String server1) {
        user.send("PONG "+server1);
    }

    @Command
    public void execute(User user, String server1, String server2) {
        if (!user.server().settings().host().equals(server2)) {
            user.send(Message.ERR_NOSUCHSERVER.client(user.info()).addFormat("", server2));
            return;
        }
        execute(user, server1);
    }

    @Command(trailing = true)
    public void executeTrailing(User user, String trailing) {
        execute(user, trailing);
    }
}

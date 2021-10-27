package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;

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
        user.send(Message.ERR_UNKNOWNERROR.client(user.info())
                                          .addFormat("command", "CONNECT")
                                          .addFormat("info", "Not supported yet"));
    }
}

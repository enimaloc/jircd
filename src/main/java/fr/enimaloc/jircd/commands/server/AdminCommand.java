package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.server.JIRCD;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;
import java.util.Optional;
import java.util.regex.Pattern;

@Command(name = "admin")
public class AdminCommand {

    @Command
    public void execute(User user) {
        execute(user, user.server().settings().host());
    }

    @Command
    public void execute(User user, String target) {
        Pattern compile = Pattern.compile(target);
        JIRCD   server;
        Optional<User> subject = user.server()
                                     .users()
                                     .stream()
                                     .filter(u -> compile.matcher(u.info().format()).matches())
                                     .findFirst();
        if (compile.matcher(user.server().settings().host()).matches()) {
            server = user.server();
        } else if (subject.isPresent()) {
            server = subject.get().server();
        } else {
            user.send(Message.ERR_NOSUCHSERVER.client(user.info()).addFormat("server name", target));
            return;
        }

        user.send(Message.RPL_ADMINME.client(user.info()).addFormat("server", server.settings().host()));
        user.send(Message.RPL_ADMINLOC1.client(user.info()).addFormat("info", server.settings().admin().loc1()));
        user.send(Message.RPL_ADMINLOC2.client(user.info()).addFormat("info", server.settings().admin().loc2()));
        user.send(Message.RPL_ADMINEMAIL.client(user.info()).addFormat("info", server.settings().admin().email()));
    }
}

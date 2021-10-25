package com.github.enimaloc.irc.jircd.commands.server;

import com.github.enimaloc.irc.jircd.server.JIRCD;
import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;
import java.util.Optional;
import java.util.regex.Pattern;

@Command(name = "info")
public class InfoCommand {

    @Command
    public void execute(User user) {
        execute(user, user.server().settings().host);
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
        if (compile.matcher(user.server().settings().host).matches()) {
            server = user.server();
        } else if (subject.isPresent()) {
            server = subject.get().server();
        } else {
            user.send(Message.ERR_NOSUCHSERVER.parameters(user.info().format(), target));
            return;
        }

        for (String info : server.infos()) {
            user.send(Message.RPL_INFO.parameters(info));
        }
        user.send(Message.RPL_ENDOFINFO);
    }

}

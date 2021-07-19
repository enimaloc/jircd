package com.github.enimaloc.irc.jircd.internal.commands.connection;

import com.github.enimaloc.irc.jircd.api.ServerSettings;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import com.github.enimaloc.irc.jircd.internal.exception.IRCException;
import java.util.Optional;

@Command(name = "oper")
public class OperCommand {

//    FIXME : Missing documentation about that
//    @Command
//    public void execute(User user) {
//        throw new IRCException.NoNicknameGiven()
//    }

    // TODO: 17/07/2021 ERR_NOOPERHOST - 491
    @Command
    public void execute(User user, String name, String password) {
        Optional<ServerSettings.Operator> operOptional = user.server().settings().operators.stream().filter(
                op -> op.username().equalsIgnoreCase(name)).findFirst();
        if (operOptional.isEmpty()) {
            return;
        }
        ServerSettings.Operator oper = operOptional.get();
        if (!oper.password().equals(password)) {
            throw new IRCException.PasswdMismatch(user.server().settings(), user.info());
        }
        user.info().setOper(oper);
        user.send(user.info().format() + " :You are now an IRC operator");
    }

}

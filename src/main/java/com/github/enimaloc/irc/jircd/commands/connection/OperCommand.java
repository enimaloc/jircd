package com.github.enimaloc.irc.jircd.commands.connection;

import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.server.ServerSettings;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;
import java.util.Optional;

@Command(name = "oper")
public class OperCommand {

//    FIXME : Missing documentation about that
//    @Command
//    public void execute(User user) {
//        ERR_NONICKNAMEGIVEN
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
            user.send(Message.ERR_PASSWDMISMATCH.parameters(user.info().format()));
            return;
        }
        user.info().setOper(oper);
        user.send(Message.RPL_YOUREOPER.parameters(user.info().format()));
    }

}

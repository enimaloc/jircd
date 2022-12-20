package fr.enimaloc.jircd.commands.connection;

import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.server.ServerSettings;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;
import java.util.Optional;

@Command(name = "oper")
public class OperCommand {


    @Command
    public void execute(User user, String name, String password) {
        Optional<ServerSettings.Operator> operOptional = user.server().settings().operators().stream().filter(
                op -> op.username().equalsIgnoreCase(name)).findFirst();
        if (operOptional.isEmpty()) {
            return;
        }
        ServerSettings.Operator oper = operOptional.get();
        if (!oper.password().equals(password)) {
            user.send(Message.ERR_PASSWDMISMATCH.client(user.info()));
            return;
        }
        if (!user.info().host().matches(oper.host().toRegex())) {
            user.send(Message.ERR_NOOPERHOST.client(user.info()));
            return;
        }
        user.info().setOper(oper);
        user.send(Message.RPL_YOUREOPER.client(user.info()));
    }

}

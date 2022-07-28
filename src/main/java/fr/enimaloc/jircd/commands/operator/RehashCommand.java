/*
 * RehashCommand
 *
 * 0.0.1
 *
 * 28/07/2022
 */
package fr.enimaloc.jircd.commands.operator;

import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;
import java.nio.file.Path;

/**
 *
 */
@Command(name = "rehash")
public class RehashCommand {

    @Command
    public void execute(User user) {
        if (!user.modes().oper()) {
            user.send(Message.ERR_NOPRIVILEGES.client(user.info()));
            return;
        }
        user.server().settings().reload(Path.of("settings.toml"));
        user.send(Message.RPL_REHASHING.client(user.info()).addFormat("file", "settings.toml"));
    }

}

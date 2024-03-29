package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.message.Regex;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;
import java.util.Optional;

@Command(name = "part")
public class PartCommand {

    @Command()
    public void execute(User user, String channelsRaw) {
        execute(user, channelsRaw, null);
    }

    @Command(trailing = true)
    public void execute(User user, String channelsRaw, String reason) {
        String[] channelsNames = new String[1];
        if (channelsRaw.contains(",")) {
            channelsNames = channelsRaw.split(",");
        } else {
            channelsNames[0] = channelsRaw;
        }
        for (String channelName : channelsNames) {
            Optional<Channel> channelOpt = user.channels()
                                               .stream()
                                               .filter(channel -> channel.name().equals(channelName))
                                               .findFirst();
            if (!Regex.CHANNEL.matcher(channelName).matches() || channelOpt.isEmpty()) {
                if (!Regex.CHANNEL.matcher(channelName).matches()) {
                    user.send(Message.ERR_NOSUCHCHANNEL.client(user.info()).channel(channelName));
                } else {
                    user.send(Message.ERR_NOTONCHANNEL.client(user.info()).channel(channelName));
                }
                continue;
            }
            Channel channelObj = channelOpt.get();

            channelObj.broadcast(
                    ":%s PART %s%s".formatted(user.info().format(),
                                              channelName,
                                              reason == null || reason.isEmpty() || reason.isBlank() ?
                                                      "" :
                                                      " :" + reason)
            );
            channelObj.removeUser(user);
        }
    }
}

package com.github.enimaloc.irc.jircd.commands.channel;

import com.github.enimaloc.irc.jircd.channel.Channel;
import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.message.Regex;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;
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
            if (!Regex.CHANNEL.matcher(channelName).matches()) {
                user.send(Message.ERR_NOSUCHCHANNEL.client(user.info()).addFormat("channel", channelName));
                continue;
            }
            Optional<Channel> channelOpt = user.channels()
                                               .stream()
                                               .filter(channel -> channel.name().equals(channelName))
                                               .findFirst();
            if (channelOpt.isEmpty()) {
                user.send(Message.ERR_NOTONCHANNEL.client(user.info()).addFormat("channel", channelName));
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
            user.modifiableChannels().remove(channelObj);
            channelObj.modifiableUsers().remove(user);
            if (channelObj.users().isEmpty()) {
                user.server().originalChannels().remove(channelObj);
            }
        }
    }
}

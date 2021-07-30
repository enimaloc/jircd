package com.github.enimaloc.irc.jircd.internal.commands.channel;

import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.ChannelImpl;
import com.github.enimaloc.irc.jircd.internal.JIRCDImpl;
import com.github.enimaloc.irc.jircd.internal.Regex;
import com.github.enimaloc.irc.jircd.internal.UserImpl;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
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
                user.send(Message.ERR_NOSUCHCHANNEL.parameters(user.info().format(), channelName));
                continue;
            }
            Optional<Channel> channelOpt = user.channels()
                                               .stream()
                                               .filter(channel -> channel.name().equals(channelName))
                                               .findFirst();
            if (channelOpt.isEmpty()) {
                user.send(Message.ERR_NOTONCHANNEL.parameters(user.info().format(), channelName));
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
            ((UserImpl) user).modifiableChannels().remove(channelObj);
            ((ChannelImpl) channelObj).modifiableUsers().remove(user);
            if (channelObj.users().isEmpty()) {
                ((JIRCDImpl) user.server()).originalChannels().remove(channelObj);
            }
        }
    }
}

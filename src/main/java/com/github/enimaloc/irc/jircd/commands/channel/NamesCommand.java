package com.github.enimaloc.irc.jircd.commands.channel;

import com.github.enimaloc.irc.jircd.channel.Channel;
import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.user.User;
import java.util.Optional;
import java.util.stream.Collectors;

@Command(name = "names")
public class NamesCommand {

    @Command
    public void execute(User user) {
        execute(user, user.server().channels().stream().map(Channel::name).collect(Collectors.joining(",")));
    }

    @Command
    public void execute(User user, String channelsRaw) {
        String[] channels;
        if (channelsRaw.contains(",")) {
            channels = channelsRaw.split(",");
        } else {
            channels = new String[]{channelsRaw};
        }
        for (String channelName : channels) {
            Optional<Channel> channelOpt = user.server()
                                               .channels()
                                               .stream()
                                               .filter(channel -> channel.name().equals(channelName))
                                               .findFirst();
            boolean userIn = user.channels().stream().anyMatch(channel -> channel.name().equals(channelName));

            Channel channel;
            if (channelOpt.isPresent() && (!(channel = channelOpt.get()).modes().secret() || userIn)) {
                String nicknames = channel.users()
                                          .stream()
                                          .filter(u -> !u.modes().invisible() || userIn)
                                          .map(u -> channel.prefix(u) + u.info().format())
                                          .collect(Collectors.joining(" "));
                user.send(Message.RPL_NAMREPLY.client(user.info())
                                              .addFormat("symbol", channel.modes().secret() ? "@" :
                                                      channel.modes().password().isPresent() ? "*" : "=")
                                              .addFormat("channel", channelName)
                                              .addFormat("nicknames", nicknames));
            }
            user.send(Message.RPL_ENDOFNAMES.client(user.info()).addFormat("channel", channelName));
        }
    }

}

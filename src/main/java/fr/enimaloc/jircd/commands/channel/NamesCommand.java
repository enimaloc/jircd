package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;
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

            if (channelOpt.isPresent() && (!channelOpt.get().modes().secret() || userIn)) {
                Channel channel = channelOpt.get();
                String nicknames = channel.users()
                                          .stream()
                                          .filter(u -> !u.modes().invisible() || userIn)
                                          .map(u -> channel.prefix(u) + u.modes().prefix() + u.info().format())
                                          .collect(Collectors.joining(" "));
                user.send(Message.RPL_NAMREPLY.client(user.info())
                                              .channel(channelName)
                                              .addFormat("symbol", channel.modes().secret() ? "@" :
                                                      channel.modes().password().map(s -> "*").orElse("="))
                                              .addFormat("nicknames", nicknames));
            }
            user.send(Message.RPL_ENDOFNAMES.client(user.info()).channel(channelName));
        }
    }

}

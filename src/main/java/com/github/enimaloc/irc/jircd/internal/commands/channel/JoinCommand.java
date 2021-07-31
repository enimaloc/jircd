package com.github.enimaloc.irc.jircd.internal.commands.channel;

import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.Mask;
import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.ChannelImpl;
import com.github.enimaloc.irc.jircd.internal.JIRCDImpl;
import com.github.enimaloc.irc.jircd.internal.Regex;
import com.github.enimaloc.irc.jircd.internal.UserImpl;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

@Command(name = "join")
public class JoinCommand {

    @Command
    public void execute(User user, String channelsRaw) throws InvocationTargetException, IllegalAccessException {
        execute(user, channelsRaw, "");
    }

    @Command
    public void execute(User user, String channelsRaw, String passwdRaw)
            throws InvocationTargetException, IllegalAccessException {
        if (channelsRaw.equals("0")) {
            ((UserImpl) user).process(
                    "PART " + user.channels().stream().map(Channel::name).collect(Collectors.joining(",")));
            return;
        }
        String[] channels = new String[1];
        String[] passwds  = new String[1];
        if (channelsRaw.contains(",")) {
            channels = channelsRaw.split(",");
        } else {
            channels[0] = channelsRaw;
        }
        if (passwdRaw.contains(",")) {
            passwds = new String[channels.length];
            String[] temp = passwdRaw.split(",");
            System.arraycopy(temp, 0, passwds, 0, temp.length);
        } else if (passwdRaw.isEmpty() || passwdRaw.isBlank()) {
            passwds = new String[channels.length];
        } else {
            passwds[0] = passwdRaw;
        }
        for (int i = 0; i < channels.length; i++) {
            String channel = channels[i];
            String passwd  = passwds[i];
            if (!Regex.CHANNEL.matcher(channel).matches()) {
                user.send(Message.ERR_NOSUCHCHANNEL.parameters(user.info().format(), channel));
                continue;
            }
            Channel channelObj = user.server()
                                     .channels()
                                     .stream()
                                     .filter(c -> c.name().equals(channel))
                                     .findFirst()
                                     .orElseGet(() -> {
                                         Channel temp = new ChannelImpl(user, channel);
                                         ((JIRCDImpl) user.server()).originalChannels().add(temp);
                                         return temp;
                                     });
            if (user.channels().size() >= user.server().supportAttribute().channelLen()) {
                user.send(Message.ERR_TOOMANYCHANNELS.parameters(user.info().format(), channel));
                continue;
            }
            if (channelObj.modes().password().isPresent() && !channelObj.modes().password().get().equals(passwd)) {
                user.send(Message.ERR_BADCHANNELKEY.parameters(user.info().format(), channel));
                continue;
            }
            if (channelObj.modes()
                          .bans()
                          .stream()
                          .anyMatch(mask -> new Mask(mask).toPattern().matcher(user.info().full()).matches()) &&
                channelObj.modes()
                          .except()
                          .stream()
                          .noneMatch(mask -> new Mask(mask).toPattern().matcher(user.info().full()).matches())) {
                user.send(Message.ERR_BANNEDFROMCHAN.parameters(user.info().format(), channel));
                continue;
            }
            if (channelObj.users().size() >= channelObj.modes().limit().orElse(Integer.MAX_VALUE)) {
                user.send(Message.ERR_CHANNELISFULL.parameters(user.info().format(), channel));
                continue;
            }
            if (channelObj.modes().inviteOnly()) {
                user.send(Message.ERR_INVITEONLYCHAN.parameters(user.info().format(), channel));
                continue;
            }
            ((UserImpl) user).modifiableChannels().add(channelObj);
            ((ChannelImpl) channelObj).modifiableUsers().add(user);

            channelObj.broadcast(user.info().format(), Message.CMD_JOIN.parameters(channel));
            channelObj.topic()
                      .ifPresent(topic -> user.send(
                              Message.RPL_TOPIC.parameters(user.info().format(), channel, topic.topic())));

            user.send(Message.RPL_NAMREPLY.parameters(user.info().format(),
                                                      channelObj.modes().secret() ? "@" : "=",
                                                      channelObj.name(),
                                                      channelObj.users().stream()
                                                                .map(u -> channelObj.prefix(u) +
                                                                          u.info().nickname())
                                                                .collect(Collectors.joining(" "))));
            user.send(Message.RPL_ENDOFNAMES.parameters(user.info().format(), channelObj.name()));
        }
    }

}

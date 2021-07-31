package com.github.enimaloc.irc.jircd.internal.commands.channel;

import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.Regex;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import java.util.Optional;

@Command(name = "topic")
public class TopicCommand {

    @Command
    public void execute(User user, String channelName) {
        execute(user, channelName, null);
    }

    @Command(trailing = true)
    public void execute(User user, String channelName, String topic) {
        if (!Regex.CHANNEL.matcher(channelName).matches()) {
            user.send(Message.ERR_NOSUCHCHANNEL.parameters(user.info().format(), channelName));
            return;
        }

        Optional<Channel> channelOpt = user.channels()
                                           .stream()
                                           .filter(channel -> channel.name().contains(channelName))
                                           .findFirst();
        if (channelOpt.isEmpty()) {
            user.send(Message.ERR_NOTONCHANNEL.parameters(user.info().format(), channelName));
            return;
        }

        Channel channel = channelOpt.get();
        if (topic == null) {
            Optional<Channel.Topic> topicOpt = channel.topic();
            if (topicOpt.isEmpty()) {
                user.send(Message.RPL_NOTOPIC.parameters(user.info().format(), channelName));
                return;
            }
            Channel.Topic topicObj = topicOpt.get();
            user.send(Message.RPL_TOPIC.parameters(user.info().format(), channelName, topicObj.topic()));
            user.send(Message.RPL_TOPICWHOTIME.parameters(user.info().format(), channelName,
                                                          topicObj.user().info().nickname(), topicObj.unixTimestamp()));
            return;
        }

        if (!channel.prefix(user).matches("[@%]") && channel.modes()._protected()) {
            user.send(Message.ERR_CHANOPRIVSNEEDED.parameters(user.info().format(), channelName));
            return;
        }
        channel.topic(topic.isEmpty() || topic.isBlank() ? null : new Channel.Topic(topic, user));

        Optional<Channel.Topic> topicOpt = channel.topic();
        if (topicOpt.isEmpty()) {
            channel.broadcast(user.server().settings().host,
                              Message.RPL_NOTOPIC.parameters(user.info().format(), channelName));
            return;
        }
        Channel.Topic topicObj = topicOpt.get();
        channel.broadcast(user.server().settings().host,
                          Message.RPL_TOPIC.parameters(user.info().format(), channelName, topicObj.topic()));
    }

}

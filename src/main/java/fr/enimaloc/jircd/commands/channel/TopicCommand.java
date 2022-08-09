package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.message.Regex;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;
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
            user.send(Message.ERR_NOSUCHCHANNEL.client(user.info()).addFormat("channel", channelName));
            return;
        }

        Optional<Channel> channelOpt = user.channels()
                                           .stream()
                                           .filter(channel -> channel.name().contains(channelName))
                                           .findFirst();
        if (channelOpt.isEmpty()) {
            user.send(Message.ERR_NOTONCHANNEL.client(user.info()).addFormat("channel", channelName));
            return;
        }

        Channel channel = channelOpt.get();
        if (topic == null) {
            Optional<Channel.Topic> topicOpt = channel.topic();
            if (topicOpt.isEmpty()) {
                user.send(Message.RPL_NOTOPIC.client(user.info()).addFormat("channel", channelName));
                return;
            }
            Channel.Topic topicObj = topicOpt.get();
            user.send(Message.RPL_TOPIC.client(user.info())
                                       .addFormat("channel", channelName)
                                       .addFormat("topic", topicObj.topic()));
            user.send(Message.RPL_TOPICWHOTIME.client(user.info())
                                              .addFormat("channel", channelName)
                                              .addFormat("nick", topicObj.user().info().nickname())
                                              .addFormat("setat", topicObj.unixTimestamp()));
            return;
        }

        if (!(channel.prefix(user) + user.modes().prefix()).matches("[@%]") && channel.modes()._protected()) {
            user.send(Message.ERR_CHANOPRIVSNEEDED.client(user.info()).addFormat("channel", channelName));
            return;
        }
        channel.topic(topic.isEmpty() || topic.isBlank() ? null : new Channel.Topic(topic, user));

        Optional<Channel.Topic> topicOpt = channel.topic();
        if (topicOpt.isEmpty()) {
            channel.broadcast(user.server().settings().host(),
                              Message.RPL_NOTOPIC.client(user.info()).addFormat("channel", channelName));
            return;
        }
        Channel.Topic topicObj = topicOpt.get();
        channel.broadcast(user.server().settings().host(),
                          Message.RPL_TOPIC.client(user.info()).addFormat("channel", channelName)
                                           .addFormat("topic", topicObj.topic()));
    }

}

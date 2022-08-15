/*
 * KickCommand
 *
 * 0.0.1
 *
 * 19/07/2022
 */
package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.message.Regex;
import fr.enimaloc.jircd.user.User;
import java.util.Optional;

/**
 *
 */
@Command(name = "kick")
public class KickCommand {

    @Command
    public void execute(User user, String channel, String users) {
        execute(user, channel, users, "Kicked by " + user.info().nickname());
    }

    @Command(trailing = true)
    public void execute(User user, String channelName, String users, String reason) {
        if (!Regex.CHANNEL.matcher(channelName).matches()) {
            user.send(Message.ERR_NOSUCHCHANNEL.client(user.info()).channel(channelName));
            return;
        }
        Optional<Channel> channelOpt = user.channels()
                                           .stream()
                                           .filter(channel -> channel.name().equals(channelName))
                                           .findFirst();
        if (channelOpt.isEmpty()) {
            user.send(Message.ERR_NOTONCHANNEL.client(user.info()).channel(channelName));
            return;
        }

        Channel channelObj = channelOpt.get();
        if (!channelObj.isRanked(user, Channel.Rank.HALF_OPERATOR)) {
            user.send(Message.ERR_CHANOPRIVSNEEDED.client(user.info()).channel(channelObj));
            return;
        }

        String[] usersNames = new String[1];
        if (users.contains(",")) {
            usersNames = users.split(",");
        } else {
            usersNames[0] = users;
        }
        for (String userName : usersNames) {
            Optional<User> userOpt = channelObj.users()
                                               .stream()
                                               .filter(user1 -> user1.info().nickname().equals(userName))
                                               .findFirst();
            if (userOpt.isEmpty() || channelObj.isRanked(userOpt.get(), Channel.Rank.PROTECTED)) {
                if (userOpt.isEmpty()) {
                    user.send(Message.ERR_USERNOTINCHANNEL.client(user.info())
                                                          .addFormat("nick", userName)
                                                          .addFormat("channel", channelName));
                }
                continue;
            }
            User userObj = userOpt.get();
            channelObj.broadcast(
                    ":%s KICK %s %s%s".formatted(user.info().format(),
                                                 channelName,
                                                 userObj.info().nickname(),
                                                 " :" + reason)
            );
            channelObj.removeUser(userObj);
        }
    }

}

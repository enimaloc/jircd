package com.github.enimaloc.irc.jircd.internal.commands.messages;

import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.Mask;
import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.Regex;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import java.util.Optional;
import java.util.function.Predicate;

@Command(name = "privmsg")
public class PrivmsgCommand {

    @Command
    public void execute(User user, String targets, String message) {
        executeTrailing(user, targets, message);
    }

    @Command(trailing = true)
    public void executeTrailing(User user, String targets, String trailing) {
        for (String target : (targets + ",").split(",")) {
            if (target.isEmpty()) {
                continue;
            }
            if (Regex.CHANNEL.matcher(target.replaceFirst(Regex.CHANNEL_PREFIX.pattern(), "")).matches()) {
                executeChannel(user, target, trailing);
            } else if (Regex.NICKNAME.matcher(target).matches()) {
                executeUser(user, target, trailing);
            }
        }
    }

    private void executeChannel(User user, String target, String trailing) {
        String targetWithoutPrefix = target.replaceFirst(Regex.CHANNEL_PREFIX.pattern(), "");
        String prefix              = target.replace(targetWithoutPrefix, "");
        Optional<Channel> channelOpt = user.server()
                                           .channels()
                                           .stream()
                                           .filter(c -> c.name().equals(targetWithoutPrefix))
                                           .findFirst();
        if (channelOpt.isEmpty()) {
            user.send(Message.ERR_CANNOTSENDTOCHAN.parameters(user.info().format(), targetWithoutPrefix));
            return;
        }
        Channel channel = channelOpt.get();
        if ((channel.modes().bans().stream().anyMatch(mask -> new Mask(mask).toPattern()
                                                                            .matcher(user.info().full())
                                                                            .matches()) &&
             channel.modes().except().stream().noneMatch(mask -> new Mask(mask).toPattern()
                                                                               .matcher(user.info().full())
                                                                               .matches())) ||
            (channel.modes().noExternalMessage() && !channel.users().contains(user)) ||
            (channel.modes().moderate() && !Regex.CHANNEL_PREFIX.matcher(channel.prefix(user)).matches())) {
            user.send(Message.ERR_CANNOTSENDTOCHAN.parameters(user.info().format(), targetWithoutPrefix));
            return;
        }
        Predicate<User> filter = u -> u != user;
        if (!prefix.isEmpty()) {
            StringBuilder builder = new StringBuilder("[");
            for (char c : prefix.toCharArray()) {
                switch (c) {
                    case '+':
                        builder.append("+");
                    case '%':
                        builder.append("%");
                    case '@':
                        builder.append("@");
                    case '&':
                        builder.append("&");
                    case '~':
                        builder.append("~");
                }
            }
            builder.append("]");
            filter = filter.and(u -> channel.prefix(u).matches(builder.toString()));
        }
        channel.broadcast(":" + user.info().format() + " PRIVMSG " + target + " :" + trailing, filter);
    }

    private void executeUser(User user, String target, String trailing) {
        Optional<User> targetOpt = user.server()
                                       .users()
                                       .stream()
                                       .filter(u -> u.info().format().equals(target))
                                       .findFirst();
        if (targetOpt.isEmpty()) {
            user.send(Message.ERR_NOSUCHNICK.parameters(user.info().format(), target));
            return;
        }
        User targetObj = targetOpt.get();
        if (targetObj.away().isPresent()) {
            user.send(Message.RPL_AWAY.parameters(user.info().format(), target, targetObj.away().get()));
        }
        targetObj.send(":" + user.info().format() + " PRIVMSG " + target + " :" + trailing);
    }
}

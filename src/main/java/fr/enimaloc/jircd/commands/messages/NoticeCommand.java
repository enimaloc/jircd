package fr.enimaloc.jircd.commands.messages;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.message.Mask;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.message.Regex;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;
import java.util.Optional;
import java.util.function.Predicate;

@Command(name = "notice")
public class NoticeCommand {

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
            (channel.modes().moderate() && !Regex.CHANNEL_PREFIX.matcher(channel.prefix(user) + user.modes().prefix()).matches())) {
            return;
        }
        Predicate<User> filter = u -> u != user;
        if (!prefix.isEmpty()) {
            StringBuilder builder = new StringBuilder("[");
            for (char c : prefix.toCharArray()) {
                // Switch here do not use break to pass through the labels,
                // because if a prefix is read, all the prefixes below will be added.
                switch (c) {
                    case '+':
                        builder.append("+");
                        // fallthrough
                    case '%':
                        builder.append("%");
                        // fallthrough
                    case '@':
                        builder.append("@");
                        // fallthrough
                    case '&':
                        builder.append("&");
                        // fallthrough
                    case '~':
                        builder.append("~");
                }
            }
            builder.append("]");
            filter = filter.and(u -> (channel.prefix(u) + u.modes().prefix()).matches(builder.toString()));
        }
        channel.broadcast(":" + user.info().format() + " NOTICE " + target + " :" + trailing, filter);
    }

    private void executeUser(User user, String target, String trailing) {
        Optional<User> targetOpt = user.server()
                                       .users()
                                       .stream()
                                       .filter(u -> u.info().format().equals(target))
                                       .findFirst();
        if (targetOpt.isEmpty()) {
            return;
        }
        User targetObj = targetOpt.get();
        if (targetObj.away().isPresent()) {
            user.send(Message.RPL_AWAY.client(user.info())
                                      .addFormat("nick", target)
                                      .addFormat("message", targetObj.away().get()));
        }
        targetObj.send(":" + user.info().format() + " NOTICE " + target + " :" + trailing);
    }
}

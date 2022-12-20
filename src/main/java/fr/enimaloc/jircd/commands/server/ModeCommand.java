package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.message.Regex;
import fr.enimaloc.jircd.user.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Command(name = "mode")
public class ModeCommand {

    @Command
    public void execute(User user, String target) {
        execute(user, target, null);
    }

    @Command
    public void execute(User user, String target, String modeString) {
        execute(user, target, modeString, "");
    }

    @Command
    public void execute(User user, String target, String modeString, String modeArguments) {
        if (Regex.CHANNEL.matcher(target).matches()) {
            executeChannelMode(user, target, modeString, modeArguments);
        } else if (Regex.NICKNAME.matcher(target).matches()) {
            executeUserMode(user, target, modeString);
        }
    }

    private void executeChannelMode(User user, String target, String modeString, String modeArguments) {
        Optional<Channel> channelOpt = user.server()
                                           .channels()
                                           .stream()
                                           .filter(channel -> channel.name().equals(target))
                                           .findFirst();
        if (channelOpt.isEmpty()) {
            user.send(Message.ERR_NOSUCHCHANNEL.client(user.info()).channel(target));
            return;
        }
        Channel channel = channelOpt.get();
        if (modeString == null) {
            user.send(Message.RPL_CHANNELMODEIS.client(user.info())
                                               .channel(channel)
                                               .addFormat("modestring", channel.modes().modesString())
                                               .addFormat("mode arguments", channel.modes().modesArguments()));
            user.send(Message.RPL_CREATIONTIME.client(user.info())
                                              .channel(channel)
                                              .addFormat("creationtime", channel.createAt()));
            return;
        }

        boolean empty = modeArguments.isBlank() || modeArguments.isEmpty();

        Map<Character, Predicate<Void>> onChar = new HashMap<>();
        onChar.put(null, continue0(() -> user.send(Message.ERR_UMODEUNKNOWNFLAG.client(user.info()))));
        onChar.put('b', unused -> {
            if (empty) {
                channel.modes().bans().forEach(
                        mask -> user.send(Message.RPL_BANLIST.client(user.info())
                                                             .channel(channel)
                                                             .addFormat("mask", mask)));
                user.send(Message.RPL_ENDOFBANLIST.client(user.info()).channel(channel));
                return false;
            }
            return true;
        });
        onChar.put('e', unused -> {
            if (empty) {
                channel.modes().except().forEach(
                        mask -> user.send(
                                Message.RPL_EXCEPTLIST.client(user.info())
                                                      .channel(channel)
                                                      .addFormat("mask", mask)));
                user.send(Message.RPL_ENDOFEXCEPTLIST.client(user.info()).channel(channel));
                return false;
            }
            return true;
        });
        onChar.put('I', unused -> {
            if (empty) {
                channel.modes().invEx().forEach(
                        mask -> user.send(
                                Message.RPL_INVITELIST.client(user.info())
                                                      .channel(channel)
                                                      .addFormat("mask", mask)));
                user.send(Message.RPL_ENDOFINVITELIST.client(user.info()).channel(channel));
                return false;
            }
            return true;
        });

        Map<Character, Boolean> map = channel.modes().apply(modeString, modeArguments, onChar);
        if (!map.isEmpty()) {
            channel.broadcast(
                    ":%s MODE %s %s %s".formatted(user.info().format(), target, formatNewMode(map), modeArguments).stripTrailing());
        }
    }

    private String formatNewMode(Map<Character, Boolean> diff) {
        List<Character> added   = diff.keySet().stream().filter(diff::get).toList();
        List<Character> removed = diff.keySet().stream().filter(Predicate.not(diff::get)).toList();
        return added.stream()
                    .map(c -> c + "")
                    .collect(Collectors.joining("", "+", "\0"))
                    .replaceFirst("\\+\0", "")
                    .replace("\0", "") +
               removed.stream()
                      .map(c -> c + "")
                      .collect(Collectors.joining("", "-", "\0"))
                      .replaceFirst("-\0", "")
                      .replace("\0", "");
    }

    private Predicate<Void> continue0(Runnable runnable) {
        return unused -> {
            runnable.run();
            return true;
        };
    }

    private void executeUserMode(User user, String target, String modeString) {
        if (!user.info().format().equals(target)) {
            user.send(Message.ERR_USERSDONTMATCH.client(user.info()));
            return;
        }
        if (modeString != null) {
            user.modes().apply(modeString, () -> user.send(Message.ERR_UMODEUNKNOWNFLAG.client(user.info())));
        }
        user.send(Message.RPL_UMODEIS.client(user.info()).addFormat("user modes", user.modes()));
    }
}

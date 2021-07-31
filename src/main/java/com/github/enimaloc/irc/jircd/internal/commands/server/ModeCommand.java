package com.github.enimaloc.irc.jircd.internal.commands.server;

import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.Regex;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import com.github.enimaloc.utils.NumberUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
            user.send(Message.ERR_NOSUCHCHANNEL.parameters(user.info().format(), target));
            return;
        }
        Channel channel = channelOpt.get();
        if (modeString == null) {
            user.send(Message.RPL_CHANNELMODEIS.parameters(user.info().format(),
                                                           target,
                                                           channel.modes().modesString(),
                                                           channel.modes().modesArguments()));
            user.send(Message.RPL_CREATIONTIME.parameters(user.info().format(), target, channel.createAt()));
            return;
        }

        List<Character> addC  = new ArrayList<>();
        List<Character> remC  = new ArrayList<>();
        boolean         add   = true;
        boolean         empty = modeArguments.isBlank() || modeArguments.isEmpty();
        for (char c : modeString.toCharArray()) {
            switch (c) {
                case '+', '-' -> add = c == '+';
                case 'b' -> {
                    if (empty) {
                        channel.modes().bans().forEach(
                                mask -> user.send(Message.RPL_BANLIST.parameters(user.info().format(), target, mask)));
                        user.send(Message.RPL_ENDOFBANLIST.parameters(user.info().format(), target));
                        return;
                    }
                    (add ? addC : remC).add(c);
                }
                case 'e' -> {
                    if (empty) {
                        channel.modes().except().forEach(
                                mask -> user.send(
                                        Message.RPL_EXCEPTLIST.parameters(user.info().format(), target, mask)));
                        user.send(Message.RPL_ENDOFEXCEPTLIST.parameters(user.info().format(), target));
                        return;
                    }
                    (add ? addC : remC).add(c);
                }
                case 'I' -> {
                    if (empty) {
                        channel.modes().invEx().forEach(
                                mask -> user.send(
                                        Message.RPL_INVITELIST.parameters(user.info().format(), target, mask)));
                        user.send(Message.RPL_ENDOFINVITELIST.parameters(user.info().format(), target));
                        return;
                    }
                    (add ? addC : remC).add(c);
                }
                case 'l', 'i', 'k', 'm', 's', 't', 'n' -> (add ? addC : remC).add(c);
                default -> user.send(Message.ERR_UMODEUNKNOWNFLAG.parameters(user.info().format()));
            }
        }
        addC.forEach(char_ -> {
            switch (char_) {
                case 'b' -> channel.modes().bans().add(modeArguments);
                case 'e' -> channel.modes().except().add(modeArguments);
                case 'I' -> channel.modes().invEx().add(modeArguments);
                case 'l' -> channel.modes().limit(NumberUtils.getSafe(modeArguments, Integer.class).orElse(0));
                case 'i' -> channel.modes().inviteOnly(true);
                case 'k' -> channel.modes().password(empty ? null : modeArguments);
                case 'm' -> channel.modes().moderate(true);
                case 's' -> channel.modes().secret(true);
                case 't' -> channel.modes()._protected(true);
                case 'n' -> channel.modes().noExternalMessage(true);
            }
        });
        remC.forEach(char_ -> {
            switch (char_) {
                case 'b' -> channel.modes().bans().remove(modeArguments);
                case 'e' -> channel.modes().except().remove(modeArguments);
                case 'I' -> channel.modes().invEx().remove(modeArguments);
                case 'l' -> channel.modes().limit(0);
                case 'i' -> channel.modes().inviteOnly(false);
                case 'k' -> channel.modes().password(null);
                case 'm' -> channel.modes().moderate(false);
                case 's' -> channel.modes().secret(false);
                case 't' -> channel.modes()._protected(false);
                case 'n' -> channel.modes().noExternalMessage(false);
            }
        });
        channel.broadcast(
                ":" + user.info().format() + " MODE " + target + " " +
                (addC.isEmpty() ? "" : "+" + addC.stream().map(c -> c + "").collect(Collectors.joining())) +
                (remC.isEmpty() ? "" : "-" + remC.stream().map(c -> c + "").collect(Collectors.joining())) +
                (empty ? "" : " " + (remC.contains('k') ? "*" : modeArguments))
        );
    }

    private void executeUserMode(User user, String target, String modeString) {
        if (!user.info().format().equals(target)) {
            user.send(Message.ERR_USERSDONTMATCH.parameters(user.info().format()));
            return;
        }
        if (modeString != null) {
            boolean add = true;
            for (char c : modeString.toCharArray()) {
                switch (c) {
                    case '+', '-' -> add = c == '+';
                    case 'i' -> user.modes().invisible(add);
                    case 'o' -> user.modes().oper(false);
                    case 'O' -> user.modes().localOper(false);
//                    case 'r' -> user.modes().registered(add);
                    case 'w' -> user.modes().wallops(add);
                    default -> user.send(Message.ERR_UMODEUNKNOWNFLAG.parameters(user.info().format()));
                }
            }
        }
        user.send(Message.RPL_UMODEIS.parameters(user.info().format(), user.modes().modes()));
    }
}

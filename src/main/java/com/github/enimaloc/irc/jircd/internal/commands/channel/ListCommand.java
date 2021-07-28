package com.github.enimaloc.irc.jircd.internal.commands.channel;

import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import com.github.enimaloc.utils.NumberUtils;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Command(name = "list")
public class ListCommand {

    @Command
    public void execute(User user) {
        executeWithChannel(user, user.server()
                                     .channels()
                                     .stream()
                                     .filter(channel -> !channel.modes().secret())
                                     .map(Channel::name)
                                     .collect(Collectors.joining(",")));
    }

    @Command
    public void execute(User user, String raw) {
        if (raw.startsWith("C<") || raw.startsWith("C>") ||
            raw.startsWith("T<") || raw.startsWith("T>") ||
            raw.startsWith("<") || raw.startsWith(">") ||
            user.server()
                .channels()
                .stream()
                .map(Channel::name)
                .noneMatch(name -> Arrays.stream((raw + ",").split(","))
                                         .toList()
                                         .contains(name))
        ) {
            executeWithEList(user, raw);
        } else {
            executeWithChannel(user, raw);
        }
    }

    private void executeWithChannel(User user, String raw) {
        execute(user, raw, "");
    }

    private void executeWithEList(User user, String raw) {
        execute(user, user.server()
                          .channels()
                          .stream()
                          .filter(channel -> !channel.modes().secret())
                          .map(Channel::name)
                          .collect(Collectors.joining(",")), raw);
    }

    @Command
    public void execute(User user, String channelsRaw, String eListRaw) {
        System.out.println("user = " + user + ", channelsRaw = " + channelsRaw + ", eListRaw = " + eListRaw);
        Predicate<Channel> predicate = channel -> true;
        String[]           eList;
        if (eListRaw == null || eListRaw.isBlank() || eListRaw.isEmpty()) {
            eList = new String[0];
        } else if (eListRaw.contains(",")) {
            eList = eListRaw.split(",");
        } else {
            eList = new String[]{eListRaw};
        }
        for (String e : eList) {
            char[] chars       = e.toCharArray();
            int    i           = 0;
            char   identifier  = chars.length >= 3 ? Character.toUpperCase(chars[i++]) : '\u0000'; // ignored for now
            char   comparaison = Character.toUpperCase(chars[i++]);
            int    b           = NumberUtils.getSafe(e.substring(i), Integer.class).orElse(Integer.MIN_VALUE);

            predicate = switch (comparaison) {
                case '>' -> switch (identifier) {
//                    case 'C' -> predicate.and(channel -> channel.)
//                    case 'T' -> predicate.and(channel -> channel.topic().isPresent() &&
//                                                         channel.topic().get().unixTimestamp() > b);
                    case '\u0000' -> predicate.and(channel -> channel.users().size() > b);
                    default -> throw new IllegalStateException("Unexpected value: " + identifier);
                };
                case '<' -> switch (identifier) {
//                    case 'C' -> predicate.and(channel -> channel.)
//                    case 'T' -> predicate.and(channel -> channel.topic().isPresent() &&
//                                                         channel.topic().get().unixTimestamp() < b);
                    case '\u0000' -> predicate.and(channel -> channel.users().size() < b);
                    default -> throw new IllegalStateException("Unexpected value: " + identifier);
                };
                default -> predicate.and(channel -> Pattern.compile(e).matcher(channel.name()).matches());
            };
        }

        String[] channels;
        if (channelsRaw.contains(",")) {
            channels = channelsRaw.split(",");
        } else {
            channels = new String[]{channelsRaw};
        }
        user.send(Message.RPL_LISTSTART.parameters(user.info().format()));
        for (String channelName : channels) {
            user.server()
                .channels()
                .stream()
                .filter(predicate.and(channel -> channel.name().equals(channelName)))
                .map(channel -> Message.RPL_LIST.parameters(user.info().format(),
                                                            channel.name(),
                                                            channel.users().size(),
                                                            channel.topic()
                                                                   .orElse(new Channel.Topic("", null))
                                                                   .topic()))
                .forEach(user::send);
        }
        user.send(Message.RPL_LISTEND.parameters(user.info().format()));
    }

}

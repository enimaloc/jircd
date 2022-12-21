package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.message.Mask;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.message.Regex;
import fr.enimaloc.jircd.user.User;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
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
            user.process(
                    "PART " + user.channels().stream().map(Channel::name).collect(Collectors.joining(",")));
            return;
        }
        for (Couple couple : couples(channelsRaw, passwdRaw)) {
            actionPerChannel(user, couple);
        }
    }

    private void actionPerChannel(User user, Couple couple) {
        if (!Regex.CHANNEL.matcher(couple.name()).matches()) {
            user.send(Message.ERR_NOSUCHCHANNEL.client(user.info()).channel(couple.name()));
            return;
        }

        Channel channelObj = user.server()
                                 .channels()
                                 .stream()
                                 .filter(c -> c.name().equals(couple.name()))
                                 .findFirst()
                                 .orElseGet(() -> {
                                     Channel temp = new Channel(user, couple.name());
                                     user.server().originalChannels().add(temp);
                                     return temp;
                                 });

        Optional<Message> error = invalid(user, couple, channelObj);
        if (error.isPresent()) {
            user.send(error.get().client(user.info()).channel(couple.name()));
            return;
        }

        channelObj.addUser(user);
    }

    private Optional<Message> invalid(User user, Couple couple, Channel channelObj) {
        Message error = null;
        if (user.channels().size() >= user.server().supportAttribute().channelAttribute().channelLen()) {
            error = Message.ERR_TOOMANYCHANNELS;
        }
        if (channelObj.modes().password().isPresent() && !channelObj.modes().password().equals(couple.password())) {
            error = Message.ERR_BADCHANNELKEY;
        }
        if (channelObj.modes()
                      .bans()
                      .stream()
                      .anyMatch(mask -> new Mask(mask).toPattern().matcher(user.info().full()).matches()) &&
            channelObj.modes()
                      .except()
                      .stream()
                      .noneMatch(mask -> new Mask(mask).toPattern().matcher(user.info().full()).matches())) {
            error = Message.ERR_BANNEDFROMCHAN;
        }
        if (channelObj.users().size() >= channelObj.modes().limit().orElse(Integer.MAX_VALUE)) {
            error = Message.ERR_CHANNELISFULL;
        }
        if (channelObj.modes().inviteOnly()) {
            error = Message.ERR_INVITEONLYCHAN;
        }
        return Optional.ofNullable(error);
    }

    Couple[] couples(String channelsRaw, String passwdRaw) {
        String[] channels = channelsRaw.split(",");
        String[] passwds  = passwdRaw.split(",");

        Couple[] couples = new Couple[channels.length];
        for (int i = 0; i < couples.length; i++) {
            couples[i] = new Couple(channels[i], i < passwds.length ? passwds[i] : null);
        }
        return couples;
    }

    record Couple(String name, String password0) {
        public Optional<String> password() {
            return Optional.ofNullable(password0);
        }
    }
}

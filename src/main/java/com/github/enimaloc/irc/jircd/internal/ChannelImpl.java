package com.github.enimaloc.irc.jircd.internal;

import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import java.util.*;

public class ChannelImpl implements Channel {
    private final String            name;
    private final Modes             modes;
    private final List<User>        bans;
    private final List<User>        users;
    private final Map<User, String> prefix;
    private       String            password;
    private       Channel.Topic     topic;

    public ChannelImpl() {
        this("");
    }

    public ChannelImpl(String name) {
        this(name, null);
    }

    public ChannelImpl(String name, Channel.Topic topic) {
        this(name, topic, new Modes(null, 0, false, false));
    }

    public ChannelImpl(String name, Channel.Topic topic, Modes modes) {
        this(name, topic, modes, new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    }

    public ChannelImpl(
            String name, Channel.Topic topic, Modes modes, List<User> bans, List<User> users, Map<User, String> prefix
    ) {
        this.name   = name;
        this.topic  = topic;
        this.modes  = modes;
        this.bans   = bans;
        this.users  = users;
        this.prefix = prefix;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<Topic> topic() {
        return topic != null && topic.topic() != null && topic.user() != null ? Optional.of(topic) : Optional.empty();
    }

    @Override
    public void topic(Topic topic) {
        this.topic = topic;
    }

    @Override
    public Modes modes() {
        return modes;
    }

    @Override
    public List<User> bans() {
        return Collections.unmodifiableList(bans);
    }

    public List<User> modifiableBans() {
        return bans;
    }

    @Override
    public List<User> users() {
        return Collections.unmodifiableList(users);
    }

    public List<User> modifiableUsers() {
        return users;
    }

    @Override
    public Optional<String> prefix(User user) {
        return Optional.ofNullable(prefix.get(user));
    }

    @Override
    public void broadcast(String source, Message message) {
        broadcast(message.format(source));
    }

    @Override
    public void broadcast(String message) {
        for (User user : users) {
            user.send(message);
        }
    }

    public static final class Modes {
        private String  password;
        private int     limit;
        private boolean inviteOnly;
        private boolean secret;

        public Modes(
                String password,
                int limit,
                boolean inviteOnly,
                boolean secret
        ) {
            this.password   = password;
            this.limit      = limit;
            this.inviteOnly = inviteOnly;
            this.secret     = secret;
        }

        public Optional<String> password() {
            return Optional.ofNullable(password);
        }

        public OptionalInt limit() {
            return limit < 1 ? OptionalInt.empty() : OptionalInt.of(limit);
        }

        public boolean inviteOnly() {
            return inviteOnly;
        }

        public boolean secret() {
            return secret;
        }

        public void password(String password) {
            this.password = password;
        }

        public void limit(int limit) {
            this.limit = limit;
        }

        public void inviteOnly(boolean inviteOnly) {
            this.inviteOnly = inviteOnly;
        }

        public void secret(boolean secret) {
            this.secret = secret;
        }
    }
}

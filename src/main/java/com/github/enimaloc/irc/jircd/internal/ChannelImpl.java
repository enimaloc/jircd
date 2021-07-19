package com.github.enimaloc.irc.jircd.internal;

import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.User;
import java.util.*;

public class ChannelImpl implements Channel {
    private String     name;
    private String     password;
    private String     topic;
    private Modes      modes;
    private List<User> bans;
    private List<User> users;

    public ChannelImpl() {
        this("");
    }

    public ChannelImpl(String name) {
        this(name, null);
    }

    public ChannelImpl(String name, String topic) {
        this(name, topic, new Modes(null, 0, false, false));
    }

    public ChannelImpl(String name, String topic, Modes modes) {
        this(name, topic, modes, new ArrayList<>(), new ArrayList<>());
    }

    public ChannelImpl(String name, String topic, Modes modes, List<User> bans, List<User> users) {
        this.name  = name;
        this.topic = topic;
        this.modes = modes;
        this.bans  = bans;
        this.users = users;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<String> topic() {
        return Optional.ofNullable(topic);
    }

    @Override
    public void topic(String topic) {
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
    public void broadcast(String message) {
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

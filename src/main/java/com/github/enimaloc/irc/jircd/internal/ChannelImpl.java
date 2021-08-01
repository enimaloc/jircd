package com.github.enimaloc.irc.jircd.internal;

import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import java.util.*;
import java.util.function.Predicate;

public class ChannelImpl implements Channel {
    private final String            name;
    private final Modes             modes;
    private final List<User>        bans;
    private final List<User>        users;
    private final Map<User, String> prefix;
    private final long              createdAt;
    private       String            password;
    private       Channel.Topic     topic;

    public ChannelImpl(User creator) {
        this(creator, "");
    }

    public ChannelImpl(User creator, String name) {
        this(creator, name, null);
    }

    public ChannelImpl(User creator, String name, Topic topic) {
        this(creator, name, topic, new Modes(
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                0,
                false,
                false,
                false,
                false,
                false
        ));
    }

    public ChannelImpl(User creator, String name, Topic topic, Modes modes) {
        this(creator, name, topic, modes, new ArrayList<>(), new ArrayList<>(), new HashMap<>(),
             System.currentTimeMillis() / 1000);
    }

    public ChannelImpl(
            User creator, String name, Channel.Topic topic, Modes modes, List<User> bans, List<User> users,
            Map<User, String> prefix, long createdAt
    ) {
        this.name   = name;
        this.topic  = topic;
        this.modes  = modes;
        this.bans   = bans;
        this.users  = users;
        this.prefix = prefix;
        this.prefix.put(creator, "~");
        this.createdAt = createdAt;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<Topic> topic() {
        return topic != null && topic.topic() != null && topic.user() != null && topic.unixTimestamp() != -1 ?
                Optional.of(topic) : Optional.empty();
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
    public List<User> users() {
        return Collections.unmodifiableList(users);
    }

    @Override
    public long createAt() {
        return createdAt;
    }

    public List<User> modifiableUsers() {
        return users;
    }

    @Override
    public String prefix(User user) {
        return (prefix.get(user) != null ? prefix.get(user) : "") + user.modes().prefix();
    }

    public void prefix(User user, String prefix) {
        this.prefix.put(user, prefix);
    }

    @Override
    public void broadcast(String source, Message message) {
        broadcast(source, message, user -> true);
    }

    @Override
    public void broadcast(String source, Message message, Predicate<User> filter) {
        broadcast(message.format(source), filter);
    }

    @Override
    public void broadcast(String message) {
        broadcast(message, user -> true);
    }

    @Override
    public void broadcast(String message, Predicate<User> filter) {
        new ArrayList<>(users).stream()
                              .filter(filter)
                              .forEach(user -> user.send(message));
    }

    public static final class Modes {
        private final List<String> except;
        private final List<String> bans;
        private final List<String> invEx;
        private       String       password;
        private       int          limit;
        private       boolean      moderate;
        private       boolean      inviteOnly;
        private       boolean      secret;
        private       boolean      _protected;
        private       boolean      noExternalMessage;

        public Modes(
                List<String> except, List<String> bans, List<String> invEx, String password, int limit,
                boolean moderate,
                boolean inviteOnly,
                boolean secret,
                boolean _protected,
                boolean noExternalMessage
        ) {
            this.except            = except;
            this.bans              = bans;
            this.invEx             = invEx;
            this.password          = password;
            this.limit             = limit;
            this.moderate          = moderate;
            this.inviteOnly        = inviteOnly;
            this.secret            = secret;
            this._protected        = _protected;
            this.noExternalMessage = noExternalMessage;
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

        public List<String> bans() {
            return bans;
        }

        public boolean moderate() {
            return moderate;
        }

        public Modes moderate(boolean moderate) {
            this.moderate = moderate;
            return this;
        }

        public boolean _protected() {
            return _protected;
        }

        public Modes _protected(boolean _protected) {
            this._protected = _protected;
            return this;
        }

        public boolean noExternalMessage() {
            return noExternalMessage;
        }

        public Modes noExternalMessage(boolean noExternalMessage) {
            this.noExternalMessage = noExternalMessage;
            return this;
        }

        public List<String> except() {
            return except;
        }

        public List<String> invEx() {
            return invEx;
        }

        public String modesString() {
            return
                    (!bans().isEmpty() ? "b" : "") +
                    (!except().isEmpty() ? "e" : "") +
                    (inviteOnly() ? "i" : "") +
                    (!invEx().isEmpty() ? "I" : "") +
                    (moderate() ? "m" : "") +
                    (_protected() ? "t" : "") +
                    (noExternalMessage() ? "n" : "") +
                    (secret() ? "s" : "") +
                    (limit().isPresent() ? "l" : "") +
                    (password().isPresent() ? "k" : "");
        }

        public String modesArguments() {
            return (limit().isPresent() ? limit().getAsInt() + " " : "") +
                   (password().isPresent() ? password().get() : "");
        }
    }
}

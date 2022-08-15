package fr.enimaloc.jircd.channel;

import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Channel {
    private final String            name;
    private final ChannelModes      modes;
    private final List<User>        users;
    private final Map<User, String> prefix;
    private final long              createdAt;
    private       Topic             topic;

    public Channel(User creator) {
        this(creator, "");
    }

    public Channel(User creator, String name) {
        this(creator, name, null);
    }

    public Channel(User creator, String name, Topic topic) {
        this(creator, name, topic, new ChannelModes());
    }

    public Channel(User creator, String name, Topic topic, ChannelModes modes) {
        this(creator, name, topic, modes, new ArrayList<>(), new HashMap<>(),
             System.currentTimeMillis() / 1000);
    }

    public Channel(
            User creator, String name, Topic topic, ChannelModes modes, List<User> users,
            Map<User, String> prefix, long createdAt
    ) {
        this.name   = name;
        this.topic  = topic;
        this.modes  = modes;
        this.users  = users;
        this.prefix = prefix;
        this.prefix.put(creator, "~");
        this.createdAt = createdAt;
    }

    public String name() {
        return name;
    }

    public Optional<Topic> topic() {
        return topic != null && topic.topic() != null && topic.user() != null && topic.unixTimestamp() != -1 ?
                Optional.of(topic) : Optional.empty();
    }

    public void topic(Topic topic) {
        this.topic = topic;
    }

    public ChannelModes modes() {
        return modes;
    }

    public List<User> users() {
        return Collections.unmodifiableList(users);
    }

    public long createAt() {
        return createdAt;
    }

    public List<User> modifiableUsers() {
        return users;
    }

    public void addUser(User user) {
        user.modifiableChannels().add(this);
        this.modifiableUsers().add(user);
        broadcast(user.info().format(), Message.CMD_JOIN.rawFormat(name()));
        topic().ifPresent(topic0 ->
                                  user.send(
                                          Message.RPL_TOPIC.client(user.info())
                                                           .channel(this)
                                                           .addFormat("topic", topic0.topic())));

        user.send(Message.RPL_NAMREPLY.client(user.info())
                                      .addFormat("symbol", modes().secret() ? "@" : "=")
                                      .channel(this)
                                      .addFormat("nicknames", users()
                                              .stream()
                                              .map(u -> prefix(u) + u.modes().prefix() +
                                                        u.info().nickname())
                                              .collect(Collectors.joining(" "))));
        user.send(Message.RPL_ENDOFNAMES.client(user.info()).channel(this));
    }

    public void removeUser(User user) {
        user.modifiableChannels().remove(this);
        modifiableUsers().remove(user);
        if (users().isEmpty()) {
            user.server().originalChannels().remove(this);
        }
    }

    public String prefix(User user) {
        return (prefix.get(user) != null ? prefix.get(user) : "");
    }

    public void prefix(User user, String prefix) {
        this.prefix.put(user, prefix);
    }

    public void broadcast(String source, Message message) {
        broadcast(source, message, user -> true);
    }

    public void broadcast(String source, Message message, Predicate<User> filter) {
        broadcast(message.format(source), filter);
    }

    public void broadcast(String message) {
        broadcast(message, user -> true);
    }

    public void broadcast(String message, Predicate<User> filter) {
        users.parallelStream()
             .filter(filter)
             .forEach(user -> user.send(message));
    }

    public boolean isRanked(User user, Rank rank) {
        for (int i = 0; i < Rank.values().length; i++) {
            if (prefix.get(user) != null
                && Rank.values()[i].prefix == prefix.get(user).charAt(0)
                && rank.ordinal() >= i) {
                return true;
            }
        }
        return false;
    }

    public enum Rank {
        FOUNDER('~', 'q', 5),
        PROTECTED('&', 'a', 4),
        OPERATOR('@', 'o', 3),
        HALF_OPERATOR('%', 'h', 2),
        VOICE('+', 'v', 1),
        NONE('\0', '\0', 0);

        public final char prefix;
        public final char mode;
        public final int power;

        Rank(char prefix, char mode, int power) {
            this.prefix = prefix;
            this.mode = mode;
            this.power = power;
        }

        public static Rank getByPower(int power) {
            for (Rank rank : values()) {
                if (rank.power == power) {
                    return rank;
                }
            }
            return NONE;
        }
    }

    public static final record Topic(String topic, User user, long unixTimestamp) {
        public static final Topic EMPTY = new Topic("", null, -1L);

        public Topic(String topic, User user) {
            this(topic, user, System.currentTimeMillis() / 1000);
        }
    }
}

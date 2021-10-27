package fr.enimaloc.jircd.channel;

import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.user.User;
import java.util.*;
import java.util.function.Predicate;

public class Channel {
    private final String       name;
    private final ChannelModes modes;
    private final List<User>   bans;
    private final List<User>        users;
    private final Map<User, String> prefix;
    private final long              createdAt;
    private String password;
    private Topic  topic;

    public Channel(User creator) {
        this(creator, "");
    }

    public Channel(User creator, String name) {
        this(creator, name, null);
    }

    public Channel(User creator, String name, Topic topic) {
        this(creator, name, topic, new ChannelModes(
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

    public Channel(User creator, String name, Topic topic, ChannelModes modes) {
        this(creator, name, topic, modes, new ArrayList<>(), new ArrayList<>(), new HashMap<>(),
             System.currentTimeMillis() / 1000);
    }

    public Channel(
            User creator, String name, Topic topic, ChannelModes modes, List<User> bans, List<User> users,
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

    public String prefix(User user) {
        return (prefix.get(user) != null ? prefix.get(user) : "") + user.modes().prefix();
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
        new ArrayList<>(users).stream()
                              .filter(filter)
                              .forEach(user -> user.send(message));
    }

    public static final record Topic(String topic, User user, long unixTimestamp) {
        public static final Topic EMPTY = new Topic(null, null, -1L);

        public Topic(String topic, User user) {
            this(topic, user, System.currentTimeMillis() / 1000);
        }
    }
}

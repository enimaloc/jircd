package com.github.enimaloc.irc.jircd.api;

import com.github.enimaloc.irc.jircd.internal.ChannelImpl;
import java.util.List;
import java.util.Optional;

public interface Channel {

    Optional<Topic> topic();

    void broadcast(String from, Message message);

    void broadcast(String message);

    String name();

    void topic(Topic topic);

    String prefix(User user);

    ChannelImpl.Modes modes();

    List<User> users();

    long createAt();

    record Topic(String topic, User user, long unixTimestamp) {
        public static final Topic EMPTY = new Topic(null, null, -1L);

        public Topic(String topic, User user) {
            this(topic, user, System.currentTimeMillis() / 1000);
        }
    }
}

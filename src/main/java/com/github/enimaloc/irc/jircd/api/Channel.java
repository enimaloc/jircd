package com.github.enimaloc.irc.jircd.api;

import com.github.enimaloc.irc.jircd.internal.ChannelImpl;
import java.util.List;
import java.util.Optional;

public interface Channel {
    void broadcast(String message);

    String name();

    Optional<String> topic();

    void topic(String topic);

    ChannelImpl.Modes modes();

    List<User> bans();

    List<User> users();
}

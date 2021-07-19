package com.github.enimaloc.irc.jircd.api;

import com.github.enimaloc.irc.jircd.internal.UserImpl;
import com.github.enimaloc.irc.jircd.internal.UserState;
import java.util.List;

public interface User {
    void send(String message);

    void terminate(String reason);

    UserImpl.Info info();

    UserState state();

    JIRCD server();

    List<Channel> channels();

    String prefix();
}

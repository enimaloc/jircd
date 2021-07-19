package com.github.enimaloc.irc.jircd.api;

import com.github.enimaloc.irc.jircd.internal.JIRCDImpl;
import com.github.enimaloc.irc.jircd.internal.SupportAttribute;
import java.io.IOException;
import java.util.List;

public interface JIRCD {
    void broadcast(String message);

    void shutdown();

    Channel createChannel(String name);

    ServerSettings settings();

    List<Channel> channels();

    List<User> users();

    List<Object> commands();

    SupportAttribute supportAttribute();

    class Builder {
        private ServerSettings serverSettings = new ServerSettings();

        public JIRCD build() throws IOException {
            return new JIRCDImpl(serverSettings);
        }

        public Builder withSettings(ServerSettings settings) {
            serverSettings = settings;
            return this;
        }
    }
}

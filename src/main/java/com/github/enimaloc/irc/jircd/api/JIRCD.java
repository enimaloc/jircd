package com.github.enimaloc.irc.jircd.api;

import com.github.enimaloc.irc.jircd.internal.JIRCDImpl;
import com.github.enimaloc.irc.jircd.internal.SupportAttribute;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface JIRCD {
    void broadcast(String message);

    void shutdown();

    ServerSettings settings();

    List<Channel> channels();

    List<User> users();

    Map<String, Map<Command.CommandIdentifier, Command.CommandIdentity>> commands();

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

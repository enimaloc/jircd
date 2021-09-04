package com.github.enimaloc.irc.jircd.api;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.github.enimaloc.irc.jircd.internal.JIRCDImpl;
import com.github.enimaloc.irc.jircd.internal.SupportAttribute;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface JIRCD {
    void broadcast(String message);

    void shutdown();

    ServerSettings settings();

    List<Channel> channels();

    List<User> users();

    Map<String, Map<Command.CommandIdentifier, Command.CommandIdentity>> commands();

    Map<String, Integer> commandUsage();

    Date createdAt();

    SupportAttribute supportAttribute();

    String[] infos();

    class Builder {
        private ServerSettings serverSettings = new ServerSettings();

        public JIRCD build() throws IOException {
            return new JIRCDImpl(serverSettings);
        }

        public Builder withFileSettings(File file) {
            FileConfig settings = FileConfig.of(file);
            settings.load();
            return this.withSettings(
                    new ObjectConverter().toObject(settings, ServerSettings::new)
            );
        }

        public Builder withSettings(ServerSettings settings) {
            serverSettings = settings;
            return this;
        }
    }
}

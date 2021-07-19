package com.github.enimaloc.irc.jircd.internal;

import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.JIRCD;
import com.github.enimaloc.irc.jircd.api.ServerSettings;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.connection.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JIRCDImpl extends Thread implements JIRCD {
    private final ServerSocket     serverSocket;
    private final List<UserImpl>   users      = new ArrayList<>();
    private final List<Object>     commands   = new ArrayList<>();
    private final List<Channel>    channels   = new ArrayList<>();
    private final Logger           logger     = LoggerFactory.getLogger(JIRCD.class);
    private final ServerSettings   settings;
    private final Date             createdAt  = new Date();
    private final SupportAttribute supportAttribute;
    private       boolean          isShutdown = false;

    public JIRCDImpl(ServerSettings settings) throws IOException {
        super("Server-Receiver");
        this.settings = settings;
        this.commands.addAll(Arrays.asList(
                // Connection
                new PassCommand(),
                new NickCommand(),
                new UserCommand(),
                new OperCommand(),
                new QuitCommand()
        ));

        supportAttribute = new SupportAttribute(
                200,
                "ascii",
                null,
                null,
                64,
                "#",
                null,
                '\u0000',
                null,
                64,
                '\u0000',
                255,
                null,
                0,
                0,
                settings.networkName,
                31,
                null,
                false,
                0,
                null,
                null,
                307,
                18
        );

        this.serverSocket = new ServerSocket(settings.port);
        this.start();
    }

    @Override
    public void run() {
        logger.info("Listening on port {}", settings.port);
        while (!this.isInterrupted()) {
            try {
                UserImpl user = new UserImpl(JIRCDImpl.this, serverSocket.accept());
                user.start();
                users.add(user);
            } catch (IOException e) {
                if (!(e.getMessage().equals("Socket closed") && isShutdown)) {
                    e.printStackTrace();
                }
            }
        }
        logger.info("Server stopped.");
        super.run();
    }

    @Override
    public void broadcast(String message) {
        new ArrayList<>(users).forEach(user -> user.send(message));
    }

    @Override
    public Channel createChannel(String name) {
        ChannelImpl channel = new ChannelImpl(name);
        channels.add(channel);
        return channel;
    }

    @Override
    public void shutdown() {
        logger.info("Stopping server...");
        isShutdown = true;
        new ArrayList<>(users).forEach(user -> user.terminate("Server closed."));
        this.interrupt();
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.warn("Failed to correctly shutdown server", e);
        }
    }

    @Override
    public ServerSettings settings() {
        return settings;
    }

    @Override
    public List<Channel> channels() {
        return channels;
    }

    @Override
    public List<User> users() {
        return Collections.unmodifiableList(users);
    }

    public List<UserImpl> originalUsers() {
        return users;
    }

    @Override
    public List<Object> commands() {
        return commands;
    }

    public Date createdAt() {
        return createdAt;
    }

    @Override
    public SupportAttribute supportAttribute() {
        return supportAttribute;
    }

    @Override
    public String toString() {
        return "IRCServerImpl{" +
               "serverSocket=" + serverSocket +
               ", users=" + users +
               ", settings=" + settings +
               '}';
    }
}

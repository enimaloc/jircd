package fr.enimaloc.jircd.server;

import fr.enimaloc.jircd.Constant;
import fr.enimaloc.jircd.channel.Channel;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.commands.channel.*;
import fr.enimaloc.jircd.commands.connection.*;
import fr.enimaloc.jircd.commands.messages.NoticeCommand;
import fr.enimaloc.jircd.commands.messages.PrivmsgCommand;
import fr.enimaloc.jircd.commands.miscellaneous.KillCommand;
import fr.enimaloc.jircd.commands.optional.UserhostCommand;
import fr.enimaloc.jircd.commands.server.*;
import fr.enimaloc.jircd.commands.undocumented.miscellaneous.PingCommand;
import fr.enimaloc.jircd.commands.user.WhoCommand;
import fr.enimaloc.jircd.server.attributes.SupportAttribute;
import fr.enimaloc.jircd.user.User;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JIRCD extends Thread {
    private final Map<String, Map<Command.CommandIdentifier, Command.CommandIdentity>> commands     = new HashMap<>();
    private final Map<String, Integer>                                                 commandUsage = new TreeMap<>();

    private final ServerSocket  serverSocket;
    private final List<User>    users    = new ArrayList<>();
    private final List<Channel> channels = new ArrayList<>();
    private final Logger        logger   = LoggerFactory.getLogger(JIRCD.class);
    private final ServerSettings   settings;
    private final Date             createdAt  = new Date();
    private final SupportAttribute supportAttribute;
    private       boolean          isShutdown = false;

    public JIRCD(ServerSettings settings) throws IOException {
        super("Server-Receiver");
        this.settings = settings;
        for (Object cmd : Arrays.asList(
                // Connection
                new PassCommand(),
                new NickCommand(),
                new UserCommand(),
                new OperCommand(),
                new QuitCommand(),

                // Channel Operations
                new JoinCommand(),
                new PartCommand(),
                new TopicCommand(),
                new NamesCommand(),
                new ListCommand(),
                new KickCommand(),

                // Server Queries and Commands
                new MotdCommand(),
                new VersionCommand(),
                new AdminCommand(),
                new ConnectCommand(),
                new LUserCommand(),
                new TimeCommand(),
                new StatsCommand(),
                new HelpCommand(),
                new InfoCommand(),
                new ModeCommand(),

                // Sending Messages
                new PrivmsgCommand(),
                new NoticeCommand(),

                // User Based Queries
                new WhoCommand(),

                // Optional Message
                new UserhostCommand(),

                // Miscellaneous Messages
                new KillCommand(),

                // Undocumented
                // Miscellaneous
                new PingCommand()
        )) {
            Class<?> clazz         = cmd.getClass();
            String   nameByAClazz  = "__DEFAULT__";
            boolean  clazzTrailing = false;
            if (clazz.isAnnotationPresent(Command.class)) {
                Command annotation = clazz.getAnnotation(Command.class);
                nameByAClazz  = annotation.name();
                clazzTrailing = annotation.trailing();
            }
            if (nameByAClazz.equals("__DEFAULT__")) {
                nameByAClazz = clazz.getSimpleName();
            }
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Command.class)) {
                    Command annotation    = method.getAnnotation(Command.class);
                    String  nameByAMethod = annotation.name();
                    boolean asTrailing    = clazzTrailing || annotation.trailing();
                    if (nameByAMethod.equals("__DEFAULT__")) {
                        nameByAMethod = nameByAClazz;
                    }
                    Map<Command.CommandIdentifier, Command.CommandIdentity> map = commands.getOrDefault(
                            nameByAMethod.toUpperCase(),
                            new HashMap<>()
                    );
                    map.put(new Command.CommandIdentifier(
                                    method.getParameterCount() - (asTrailing ? 2 : 1), asTrailing),
                            new Command.CommandIdentity(cmd, method));
                    commands.put(nameByAMethod.toUpperCase(), map);
                    commandUsage.put(nameByAMethod.toUpperCase(), 0);
                }
            }
        }

        supportAttribute = new SupportAttribute(
                200,
                "ascii",
                null,
                null,
                64,
                "#",
                "UM",
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
                User user = new User(JIRCD.this, serverSocket.accept());
                user.start();
                users.add(user);
            } catch (IOException e) {
                if (!(e.getMessage().equals("Socket closed") && isShutdown)) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        }
        logger.info("Server stopped.");
        super.run();
    }

    public void broadcast(String message) {
        new ArrayList<>(users).forEach(user -> user.send(message));
    }

    public void shutdown() {
        logger.info("Stopping server...");
        isShutdown = true;
        new ArrayList<>(users).forEach(user -> user.terminate("Server closed.", true));
        this.interrupt();
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.warn("Failed to correctly shutdown server", e);
        }
    }

    public ServerSettings settings() {
        return settings;
    }

    public List<Channel> channels() {
        return Collections.unmodifiableList(channels);
    }

    public List<Channel> originalChannels() {
        return channels;
    }

    public List<User> users() {
        return Collections.unmodifiableList(users);
    }

    public List<User> originalUsers() {
        return users;
    }

    public Map<String, Map<Command.CommandIdentifier, Command.CommandIdentity>> commands() {
        return commands;
    }

    public TreeMap<String, Integer> commandUsage() {
        return new TreeMap<>(commandUsage);
    }

    public Map<String, Integer> originalCommandUsage() {
        return commandUsage;
    }

    public Date createdAt() {
        return createdAt;
    }

    public String[] info() {
        return new String[]{
                Constant.NAME + " v" + Constant.VERSION,
                "by Antoine <antoine@enimaloc.fr>",
                "Source code: " + Constant.GITHUB
        };
    }

    public SupportAttribute supportAttribute() {
        return supportAttribute;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public String toString() {
        return "JIRCD{" +
               "users=" + users +
               ", channels=" + channels +
               ", settings=" + settings +
               ", createdAt=" + createdAt +
               '}';
    }
}

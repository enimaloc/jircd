package com.github.enimaloc.irc.jircd.user;

import com.github.enimaloc.irc.jircd.*;
import com.github.enimaloc.irc.jircd.channel.Channel;
import com.github.enimaloc.irc.jircd.commands.Command;
import com.github.enimaloc.irc.jircd.commands.server.VersionCommand;
import com.github.enimaloc.irc.jircd.message.Message;
import com.github.enimaloc.irc.jircd.server.JIRCD;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class User extends Thread {

    private final Socket           socket;
    private final Timer            pingTimer;
    private final BufferedReader   input;
    private final DataOutputStream output;
    private final Logger        logger   = LoggerFactory.getLogger(User.class);
    private final JIRCD         server;
    private final List<Channel> channels = new ArrayList<>();
    private final UserInfo      info;
    private final UserModes         modes;
    private       UserState        state;
    private       boolean          pingSent = false;
    private       long             nextPing;
    private       String           away;

    public User(JIRCD server, Socket socket) throws IOException {
        super("Socket-Connection-" + socket.getInetAddress().getHostAddress());
        this.server   = server;
        this.socket   = socket;
        this.state    = server.settings().pass.isEmpty() ? UserState.CONNECTED : UserState.REGISTRATION;
        this.nextPing = System.currentTimeMillis() + server.settings().pingTimeout;
        this.input    = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.output   = new DataOutputStream(socket.getOutputStream());
        this.info     = new UserInfo(this, server.settings());
        this.info.setHost(socket.getInetAddress().getHostAddress());
        this.modes     = new UserModes();
        this.pingTimer = new Timer("Socket-Ping-" + this.info.host());
        this.pingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() >= nextPing && !pingSent) {
                    logger.debug("Sent 'PING' to {}", info.host());
                    send("PING :"+server.settings().host);
                    pingSent = true;
                }
                if (pingSent && System.currentTimeMillis() >= nextPing + server.settings().timeout) {
                    terminate("Timed out");
                }
            }
        }, 100, 100);
    }

    @Override
    public void run() {
        logger.debug("New connection from {}", socket.getInetAddress().getHostAddress());
        logger.trace("State: {}", state);
        while (state != UserState.DISCONNECTED) {
            try {
                String line = input.readLine();
                logger.trace("Handle '{}'", line);
                if (line != null) {
                    nextPing = System.currentTimeMillis() + server.settings().pingTimeout;
                    pingSent = false;
                    logger.trace("Rescheduled ping for {} to {}", this.info.host(),
                                 new SimpleDateFormat().format(new Date(nextPing)));
                    process(line, false);
                }
            } catch (IOException | InvocationTargetException | IllegalAccessException e) {
                if ((e.getMessage() == null || !e.getMessage().equals("Socket closed")) &&
                    state != UserState.DISCONNECTED) {
                    terminate("Internal error");
                    e.printStackTrace();
                }
            }
        }
        logger.trace("Interrupted thread for {}", info.format());
        super.run();
    }

    public void send(Message message) {
        send(message.format(server.settings().host));
    }

    public void send(String raw) {
        try {
            logger.trace("Sent {}", raw);
            output.writeBytes(raw + "\r\n");
            output.flush();
        } catch (IOException e) {
            if (!e.getMessage().equals("Socket closed") && !server.isShutdown()) {
                e.printStackTrace();
            }
        }
    }

    public void terminate(String reason) {
        terminate(reason, false);
    }

    public void terminate(String reason, boolean kicked) {
        try {
            logger.debug("Disconnected with reason: '{}'", reason);
            state = UserState.DISCONNECTED;
            this.channels.forEach(channel -> channel.modifiableUsers().remove(this));
            this.channels.stream().filter(c -> c.users().isEmpty()).forEach(server.originalChannels()::remove);
            this.channels.forEach(
                    channel -> channel.broadcast(":" + info.format() + " QUIT :" + (kicked ? "" : "Quit: ") + reason));
            this.channels.clear();
            output.close();
            input.close();
            socket.close();
            pingTimer.cancel();
            server.originalUsers().remove(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void process(String line) throws InvocationTargetException, IllegalAccessException {
        process(line, true);
    }

    public void process(String line, boolean systemInvoke) throws InvocationTargetException, IllegalAccessException {
        String[] split   = line.contains(" ") ? line.split(" ", 2) : new String[]{line, ""};
        String   command = split[0].toUpperCase();
        String[] params  = split[1].contains(":") ? split[1].split(":", 2) : new String[]{split[1]};
        String[] middle = params.length != 0 && !(params[0].isEmpty() || params[0].isBlank()) ? params[0].contains(
                " ") ? params[0].split(" ") : new String[]{params[0]} : new String[0];
        String trailing = params.length == 2 ? params[1] : null;

        if (!server.commands().containsKey(command)) {
            return;
        }
        if (!systemInvoke) {
            server.originalCommandUsage().merge(command, 1, Integer::sum);
        }
        Map<Command.CommandIdentifier, Command.CommandIdentity> commandMap = server.commands().get(command);
        Command.CommandIdentifier identifier = new Command.CommandIdentifier(
                middle.length, trailing != null);
        if (!commandMap.containsKey(identifier)) {
            commandMap.keySet()
                      .stream()
                      .min(Comparator.comparingInt(Command.CommandIdentifier::parametersCount))
                      .ifPresent(min -> send(Message.ERR_NEEDMOREPARAMS.parameters(info.format(), command)));
            return;
        }
        Command.CommandIdentity cmd = commandMap.get(identifier);

        Object[] args = new Object[cmd.method().getParameterCount()];
        args[0] = this;
        System.arraycopy(middle, 0, args, 1, middle.length);
        if (identifier.hasTrailing()) {
            args[args.length - 1] = trailing;
        }
        cmd.method().invoke(cmd.instance(), args);
    }

    public UserState state() {
        return state;
    }

    public void state(UserState state) {
        this.state = state;
    }

    public void finishRegistration() {
        state = UserState.LOGGED;
        String userInfo = info.format();
        send(Message.RPL_WELCOME.parameters(userInfo, server.settings().networkName, userInfo));
        send(Message.RPL_YOURHOST.parameters(userInfo, Constant.NAME, Constant.VERSION));
        send(Message.RPL_CREATED.parameters(userInfo, server.createdAt(), server.createdAt()));
        send(Message.RPL_MYINFO.parameters(userInfo, Constant.NAME, Constant.VERSION, "", ""));
        VersionCommand.send_ISUPPORT(this);
        try {
            process("MOTD");
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public JIRCD server() {
        return server;
    }

    public UserInfo info() {
        return info;
    }

    public List<Channel> channels() {
        return Collections.unmodifiableList(channels);
    }

    public List<Channel> modifiableChannels() {
        return channels;
    }

    public UserModes modes() {
        return modes;
    }

    public Optional<String> away() {
        return Optional.ofNullable(away);
    }

    public void away(String away) {
        this.away = away;
    }

    @Override
    public String toString() {
        return "User{" +
               "channels=" + channels +
               ", info=" + info +
               ", state=" + state +
               ", nextPing=" + nextPing +
               '}';
    }
}

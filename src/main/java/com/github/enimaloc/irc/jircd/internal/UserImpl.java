package com.github.enimaloc.irc.jircd.internal;

import com.github.enimaloc.irc.jircd.Constant;
import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.JIRCD;
import com.github.enimaloc.irc.jircd.api.Message;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserImpl extends Thread implements User {

    private final Socket           socket;
    private final Thread           pingThread;
    private final BufferedReader   input;
    private final DataOutputStream output;
    private final Logger           logger   = LoggerFactory.getLogger(User.class);
    private final JIRCDImpl        server;
    private final List<Channel>    channels = new ArrayList<>();
    private final Info             info;
    private final Modes            modes;
    private       UserState        state;
    private       boolean          pingSent = false;
    private       long             nextPing;

    public UserImpl(JIRCDImpl server, Socket socket) throws IOException {
        super("Socket-Connection-" + socket.getInetAddress().getHostAddress());
        this.server   = server;
        this.socket   = socket;
        this.state    = server.settings().pass.isEmpty() ? UserState.CONNECTED : UserState.REGISTRATION;
        this.nextPing = System.currentTimeMillis() + server.settings().pingTimeout;
        this.input    = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.output   = new DataOutputStream(socket.getOutputStream());
        this.info     = new Info(this, server.settings());
        this.info.setHost(socket.getInetAddress().getHostAddress());
        this.modes      = new Modes();
        this.pingThread = new Thread(() -> {
            logger.trace("Scheduled ping for {} to {}", this.info.host(),
                         new SimpleDateFormat().format(new Date(nextPing)));
            while (state != UserState.DISCONNECTED) {
                if (System.currentTimeMillis() >= nextPing && !pingSent) {
                    logger.debug("Sent 'PING' to {}", this.info.host());
                    send("PING");
                    pingSent = true;
                }
                if (pingSent && System.currentTimeMillis() >= nextPing + server.settings().timeout) {
                    terminate("Timed out");
                    return;
                }
            }
        }, "Socket-Ping-" + this.info.host());
        this.pingThread.start();
    }

    @Override
    public void run() {
        logger.debug("New connection from {}", socket.getInetAddress().getHostAddress());
        logger.trace("State: {}", state);
        while (state != UserState.DISCONNECTED) {
            try {
                String line = input.readLine();
                logger.trace("Handle '{}'", line);
                if (line == null) {
                    logger.warn("Protocol violation while serving {}.", this.info.host());
                    terminate("Protocol violation");
                } else {
                    nextPing = System.currentTimeMillis() + server.settings().pingTimeout;
                    pingSent = false;
                    logger.trace("Rescheduled ping for {} to {}", this.info.host(),
                                 new SimpleDateFormat().format(new Date(nextPing)));
                    process(line);
                }
            } catch (IOException | InvocationTargetException | IllegalAccessException e) {
                if ((e.getMessage() == null || !e.getMessage().equals("Socket closed")) &&
                    state != UserState.DISCONNECTED) {
                    terminate("Internal error");
                    e.printStackTrace();
                }
                return;
            }
        }
        super.run();
    }

    @Override
    public void send(Message message) {
        send(message.format(server.settings().host));
    }

    @Override
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

    @Override
    public void terminate(String reason) {
        terminate(reason, false);
    }

    public void terminate(String reason, boolean kicked) {
        try {
            logger.debug("Disconnected with reason: '{}'", reason);
            state = UserState.DISCONNECTED;
            channels.forEach(
                    channel -> channel.broadcast(":" + info.format() + " QUIT :" + (kicked ? "" : "Quit: ") + reason));
            channels.forEach(channel -> ((ChannelImpl) channel).modifiableUsers().remove(this));
            channels.clear();
            socket.close();
            server.originalUsers().remove(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void process(String line) throws InvocationTargetException, IllegalAccessException {
        String[] split   = line.contains(" ") ? line.split(" ", 2) : new String[]{line, ""};
        String   command = split[0];
        String[] params  = split[1].contains(":") ? split[1].split(":") : new String[]{split[1]};
        String[] middle = params.length != 0 && !(params[0].isEmpty() || params[0].isBlank()) ? params[0].contains(
                " ") ? params[0].split(" ") : new String[]{params[0]} : new String[0];
        String trailing = params.length == 2 ? params[1] : null;

        int min = -1;
        for (Object cmd : server.commands()) {
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
                    if (nameByAMethod.equalsIgnoreCase(command)) {
                        min = Math.min(min == -1 ? Integer.MAX_VALUE : min,
                                       method.getParameterCount() - (asTrailing ? 2 : 1));
                        if (method.getParameterCount() - (asTrailing ? 2 : 1) == middle.length) {
                            Object[] args = new Object[method.getParameterCount()];
                            args[0] = this;
                            System.arraycopy(middle, 0, args, 1, middle.length);
                            if (asTrailing) {
                                if (trailing != null) {
                                    args[args.length - 1] = trailing;
                                } else {
                                    continue;
                                }
                            }
                            method.invoke(cmd, args);
                            return;
                        }
                    }
                }
            }
        }
        if (min > 0) {
            send(Message.ERR_NEEDMOREPARAMS.parameters(info.format(), command));
        }
    }

    @Override
    public UserState state() {
        return state;
    }

    public void finishRegistration() {
        state = UserState.LOGGED;
        String userInfo = info.format();
        send(Message.RPL_WELCOME.parameters(userInfo, server.settings().networkName, userInfo));
        send(Message.RPL_YOURHOST.parameters(userInfo, Constant.NAME, Constant.VERSION));
        send(Message.RPL_CREATED.parameters(userInfo, server.createdAt(), server.createdAt()));
        send(Message.RPL_MYINFO.parameters(userInfo, Constant.NAME, Constant.VERSION, "", ""));
        List<Map<String, Object>> tokens = server.supportAttribute()
                                                 .asMapsWithLimit(13,
                                                                  (key, value) -> {
                                                                      if (value == null) {
                                                                          return false;
                                                                      }
                                                                      if (value instanceof Boolean) {
                                                                          return (boolean) value;
                                                                      }
                                                                      if (value instanceof Character) {
                                                                          return (char) value != '\u0000';
                                                                      }
                                                                      return true;
                                                                  });
        for (Map<String, Object> token : tokens) {
            StringBuilder builder = new StringBuilder();
            if (token.isEmpty()) {
                continue;
            }
            token.forEach((s, o) -> builder.append(s.toUpperCase(Locale.ROOT))
                                           .append(parseOptional(o))
                                           .append(" "));
            send(Message.RPL_ISUPPORT.parameters(userInfo, builder.deleteCharAt(builder.length() - 1)));
        }
        if (server.settings().motd.length != 0) {
            send(Message.RPL_MOTDSTART.parameters(info.format(), server.settings().host));
            for (String s : server.settings().motd) {
                send(Message.RPL_MOTD.parameters(info.format(), s));
            }
            send(Message.RPL_ENDOFMOTD.parameters(info.format()));
        } else {
            send(Message.ERR_NOMOTD.parameters(info.format()));
        }
    }

    private String parseOptional(Object potentialOptional) {
        if (potentialOptional instanceof Optional) {
            return parseOptional_((Optional<?>) potentialOptional);
        } else if (potentialOptional instanceof OptionalInt) {
            return parseOptional_((OptionalInt) potentialOptional);
        }
        return "=" + potentialOptional;
    }

    private String parseOptional_(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<?> optional) {
        return optional.map(o -> "=" + o).orElse("");
    }

    private String parseOptional_(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") OptionalInt optional) {
        return (optional.isPresent() ? "=" + optional.getAsInt() : "");
    }

    public void setState(UserState state) {
        this.state = state;
    }

    @Override
    public JIRCD server() {
        return server;
    }

    @Override
    public Info info() {
        return info;
    }

    @Override
    public List<Channel> channels() {
        return Collections.unmodifiableList(channels);
    }

    public List<Channel> modifiableChannels() {
        return channels;
    }

    @Override
    public Modes modes() {
        return modes;
    }

    @Override
    public String toString() {
        return "UserImpl{" +
               "state=" + state +
               ", info='" + info + '\'' +
               ", pingSent=" + pingSent +
               ", nextPing=" + nextPing +
               '}';
    }
}

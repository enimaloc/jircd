package com.github.enimaloc.irc.jircd.internal;

import com.github.enimaloc.irc.jircd.Constant;
import com.github.enimaloc.irc.jircd.api.Channel;
import com.github.enimaloc.irc.jircd.api.JIRCD;
import com.github.enimaloc.irc.jircd.api.ServerSettings;
import com.github.enimaloc.irc.jircd.api.User;
import com.github.enimaloc.irc.jircd.internal.commands.Command;
import com.github.enimaloc.irc.jircd.internal.exception.IRCException;
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
        this.info     = new Info(server.settings());
        this.info.setHost(socket.getInetAddress().getHostAddress());
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
                } else if (pingSent && line.equalsIgnoreCase("PONG")) {
                    nextPing = System.currentTimeMillis() + server.settings().pingTimeout;
                    pingSent = false;
                    logger.trace("Rescheduled ping for {} to {}", this.info.host(),
                                 new SimpleDateFormat().format(new Date(nextPing)));
                } else {
                    try {
                        process(line);
                    } catch (InvocationTargetException e) {
                        try {
                            throw e.getCause();
                        } catch (IRCException exception) {
                            throw exception;
                        } catch (Throwable throwable) {
                            logger.error("This append when process command {} from {}", line, info.format(), throwable);
                            throw new IRCException.UnknownError(server.settings(), info, "", "",
                                                                throwable.getMessage());
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IRCException e) {
                send(e.format());
            } catch (IOException e) {
                if (!e.getLocalizedMessage().equals("Socket closed") && state != UserState.DISCONNECTED) {
                    terminate("Internal error");
                    e.printStackTrace();
                }
                return;
            }
        }
        super.run();
    }

    @Override
    public void send(String message) {
        try {
            logger.trace("Sent {}", message);
            output.writeBytes(message + "\r\n");
            output.flush();
        } catch (IOException e) {
            if (e.getMessage().equals("Socket closed")) {
                logger.warn("Socket close, maybe have no effect on test, ignoring", e);
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void terminate(String reason) {
        try {
            logger.debug("Disconnected with reason: '{}'", reason);
            state = UserState.DISCONNECTED;
            channels.forEach(channel -> channel.broadcast(":" + info.format() + " QUIT :Quit: " + reason));
            socket.close();
            server.originalUsers().remove(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void process(String line) throws InvocationTargetException, IllegalAccessException {
        String[] split   = line.contains(" ") ? line.split(" ", 2) : new String[]{line, ""};
        String   command = split[0];
        String[] params  = split[1].contains(":") ? split[1].split(":") : new String[]{split[1]};
        String[] middle = params.length != 0 && !(params[0].isEmpty() || params[0].isBlank()) ? params[0].contains(
                " ") ? params[0].split(" ") : new String[]{params[0]} : new String[0];
        String trailing = params.length == 2 ? params[1] : "";

        int min = -1;
        for (Object cmd : server.commands()) {
            Class<?> clazz        = cmd.getClass();
            String   nameByAClazz = "__DEFAULT__";
            boolean  asTrailing   = false;
            if (clazz.isAnnotationPresent(Command.class)) {
                Command annotation = clazz.getAnnotation(Command.class);
                nameByAClazz = annotation.name();
                asTrailing   = annotation.trailing();
            }
            if (nameByAClazz.equals("__DEFAULT__")) {
                nameByAClazz = clazz.getSimpleName();
            }
            for (Method method : clazz.getDeclaredMethods()) {
                String nameByAMethod = "__DEFAULT__";
                if (method.isAnnotationPresent(Command.class)) {
                    Command annotation = method.getAnnotation(Command.class);
                    nameByAMethod = annotation.name();
                    asTrailing    = asTrailing || annotation.trailing();
                    if (nameByAMethod.equals("__DEFAULT__")) {
                        nameByAMethod = nameByAClazz;
                    }
                    if (nameByAMethod.equalsIgnoreCase(command)) {
                        min = Math.min(min == -1 ? Integer.MAX_VALUE : min, method.getParameterCount() - (asTrailing ? 2 : 1));
                        if (method.getParameterCount() - (asTrailing ? 2 : 1) == middle.length) {
                            Object[] args = new Object[method.getParameterCount()];
                            args[0] = this;
                            System.arraycopy(middle, 0, args, 1, middle.length);
                            if (asTrailing) {
                                args[args.length - 1] = trailing;
                            }
                            method.invoke(cmd, args);
                            return;
                        }
                    }
                }
            }
        }
        if (min > 0) {
            throw new IRCException.NeedMoreParamsError(server.settings(), info, command);
        }
    }

    @Override
    public UserState state() {
        return state;
    }

    public void finishRegistration() {
        state = UserState.LOGGED;
        send("%s :Welcome to the %s Network, %s".formatted(info.format(), server.settings().networkName,
                                                           info.format()));
        send("%s :Your host is %s, running version %s".formatted(info.format(), Constant.NAME, Constant.VERSION));
        send("%s :This server was created %tD %tT".formatted(info.format(), server.createdAt(), server.createdAt()));
        send("%s %s %s %s %s".formatted(info.format(), Constant.NAME, Constant.VERSION, "", ""));
        List<Map<String, Object>> tokens = server.supportAttribute()
                                                 .asMapsWithLimit(13,
                                                                  (key, value) -> {
                                                                      if (value == null) {
                                                                          return false;
                                                                      }
                                                                      if (value instanceof Boolean) {
                                                                          return (boolean) value;
                                                                      }
                                                                      return true;
                                                                  });
        for (Map<String, Object> token : tokens) {
            StringBuilder builder = new StringBuilder(info.format() + " ");
            if (token.isEmpty()) {
                continue;
            }
            token.forEach((s, o) -> builder.append(s.toUpperCase(Locale.ROOT))
                                           .append(parseOptional(o))
                                           .append(" "));
            send(builder.append(":are supported by this server").toString());
        }
    }

    private String parseOptional(Object potentialOptional) {
        if (potentialOptional instanceof Optional) {
            return parseOptional_((Optional) potentialOptional);
        } else if (potentialOptional instanceof OptionalInt) {
            return parseOptional_((OptionalInt) potentialOptional);
        }
        return "=" + potentialOptional;
    }

    private String parseOptional_(Optional optional) {
        return (optional.isPresent() ? "=" + optional.get() : "");
    }

    private String parseOptional_(OptionalInt optional) {
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
    public String prefix() {
        return "";
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

    public class Info {

        private String                  host;
        private String                  username;
        private String                  nickname;
        private String                  realName;
        private boolean                 passwordValid;
        private ServerSettings.Operator oper;

        public Info(ServerSettings settings) {
            this.passwordValid = !hasString(settings.pass);
        }

        public String host() {
            return host;
        }

        public String username() {
            return username;
        }

        public String nickname() {
            return nickname;
        }

        public String realName() {
            return realName;
        }

        public boolean passwordValid() {
            return passwordValid;
        }

        public boolean isRegistrationComplete() {
            return hasString(host) && hasString(username) && hasString(nickname) && hasString(realName) &&
                   passwordValid && state != UserState.LOGGED;
        }

        public ServerSettings.Operator oper() {
            return oper;
        }

        public void validPass() {
            passwordValid = true;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public void setOper(ServerSettings.Operator oper) {
            this.oper = oper;
        }

        public String format() {
            return (hasString(username) ? username : "") + (hasString(host) ? (hasString(nickname) ?
                    "!" + nickname :
                    "") + "@" + host : "");
        }

        private boolean hasString(String s) {
            return s != null && !s.isEmpty() && !s.isBlank();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Info info = (Info) o;
            return Objects.equals(host, info.host) && Objects.equals(username, info.username) &&
                   Objects.equals(nickname, info.nickname) && Objects.equals(realName, info.realName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, username, nickname, realName);
        }

        @Override
        public String toString() {
            return "Info{" +
                   "host='" + host + '\'' +
                   ", username='" + username + '\'' +
                   ", nickname='" + nickname + '\'' +
                   ", realName='" + realName + '\'' +
                   '}';
        }
    }
}

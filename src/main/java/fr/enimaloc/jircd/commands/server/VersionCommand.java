package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.Constant;
import fr.enimaloc.jircd.server.JIRCD;
import fr.enimaloc.jircd.message.Message;
import fr.enimaloc.jircd.commands.Command;
import fr.enimaloc.jircd.user.User;
import java.util.*;
import java.util.regex.Pattern;

@Command(name = "version")
public class VersionCommand {

    public static void send_ISUPPORT(User user) {
        send_ISUPPORT(user, user.server());
    }

    public static void send_ISUPPORT(User user, JIRCD server) {
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
            user.send(Message.RPL_ISUPPORT.client(user.info())
                                          .addFormat("tokens", builder.deleteCharAt(builder.length() - 1)));
        }
    }

    private static String parseOptional(Object potentialOptional) {
        if (potentialOptional instanceof Optional) {
            return parseOptional_((Optional<?>) potentialOptional);
        } else if (potentialOptional instanceof OptionalInt) {
            return parseOptional_((OptionalInt) potentialOptional);
        }
        return "=" + potentialOptional;
    }

    private static String parseOptional_(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<?> optional) {
        return optional.map(o -> "=" + o).orElse("");
    }

    private static String parseOptional_(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") OptionalInt optional) {
        return (optional.isPresent() ? "=" + optional.getAsInt() : "");
    }

    @Command
    public void execute(User user) {
        execute(user, user.server().settings().host);
    }

    @Command
    public void execute(User user, String target) {
        Pattern compile = Pattern.compile(target);
        JIRCD   server;
        Optional<User> subject = user.server()
                                     .users()
                                     .stream()
                                     .filter(u -> compile.matcher(u.info().format()).matches())
                                     .findFirst();
        if (compile.matcher(user.server().settings().host).matches()) {
            server = user.server();
        } else if (subject.isPresent()) {
            server = subject.get().server();
        } else {
            user.send(Message.ERR_NOSUCHSERVER.client(user.info()).addFormat("", target));
            return;
        }

        user.send(Message.RPL_VERSION.client(user.info())
                                     .addFormat("version", Constant.VERSION)
                                     .addFormat("server", server.settings().host)
                                     .addFormat("comments", ""));
        send_ISUPPORT(user, server);
    }
}

package fr.enimaloc.jircd.server;

import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.io.WritingException;
import fr.enimaloc.jircd.message.Mask;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerSettings {

    private static final transient Logger logger = LoggerFactory.getLogger(ServerSettings.class);

    private           int            port;
    private           long           pingTimeout;
    private           long           timeout;
    private           String         pass;
    private           String         host;
    private           String         networkName;
    private           String         description;
    private           Admin          admin;
    private           List<Operator> operators;
    private           List<String>   unsafeNickname;
    private           List<String>   safeNet;
    // todo: implement permissions levels settings
    private transient String[]       motd;

    ServerSettings(Builder builder) {
        this.port           = builder.port;
        this.pingTimeout    = builder.pingTimeout;
        this.timeout        = builder.timeout;
        this.pass           = builder.pass;
        this.host           = builder.host;
        this.networkName    = builder.networkName;
        this.description    = builder.description;
        this.admin          = builder.admin;
        this.operators      = builder.operators;
        this.unsafeNickname = builder.unsafeNickname;
        this.safeNet        = builder.safeNet;
        this.motd           = builder.motd;
    }

    public ServerSettings() {
        this(new Builder());
    }

    public ServerSettings copy() {
        return copy(new ServerSettings());
    }

    public ServerSettings copy(ServerSettings to) {
        return copy(to, field -> true);
    }

    public ServerSettings copy(ServerSettings to, Predicate<Field> copyIf) {
        Arrays.stream(this.getClass().getDeclaredFields())
              .filter(copyIf.and(field -> !Modifier.isFinal(field.getModifiers())))
              .forEach(field -> {
                  if (field.trySetAccessible()) {
                      try {
                          field.set(to, field.get(this));
                      } catch (IllegalAccessException e) {
                          logger.error(e.getLocalizedMessage(), e);
                      }
                  }
              });
        return to;
    }

    public void saveAs(Path path) {
        host(); // ensure host is set
        FileConfig settings = FileConfig.of(path);
        new ObjectConverter().toConfig(this, settings);
        try {
            settings.save();
        } catch (WritingException e) {
            logger.error("Failed to save settings", e);
        }
        settings.close();
    }

    public void reload(Path path) {
        if (!Files.exists(path)) {
            new ServerSettings().saveAs(path);
        }
        try (FileConfig settings = FileConfig.of(path)) {
            settings.load();
            new ObjectConverter().toObject(settings, this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServerSettings that = (ServerSettings) o;

        return port == that.port
               && pingTimeout == that.pingTimeout
               && timeout == that.timeout
               && Objects.equals(pass, that.pass)
               && Objects.equals(host(), that.host())
               && Objects.equals(networkName, that.networkName)
               && Objects.equals(description, that.description)
               && Objects.equals(admin, that.admin)
               && Objects.equals(operators, that.operators)
               && Objects.equals(unsafeNickname, that.unsafeNickname)
               && Objects.equals(safeNet, that.safeNet)
               && Arrays.equals(motd, that.motd);
    }

    @Override
    public int hashCode() {
        int result = port;
        result = 31 * result + (int) (pingTimeout ^ (pingTimeout >>> 32));
        result = 31 * result + (int) (timeout ^ (timeout >>> 32));
        result = 31 * result + Objects.hashCode(pass);
        result = 31 * result + Objects.hashCode(host());
        result = 31 * result + Objects.hashCode(networkName);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Objects.hashCode(admin);
        result = 31 * result + Objects.hashCode(operators);
        result = 31 * result + Objects.hashCode(unsafeNickname);
        result = 31 * result + Objects.hashCode(safeNet);
        result = 31 * result + Arrays.hashCode(motd);
        return result;
    }

    public int port() {
        return port;
    }

    public long pingTimeout() {
        return pingTimeout;
    }

    public long timeout() {
        return timeout;
    }

    public Optional<String> pass() {
        return Optional.ofNullable(pass.isEmpty() ? null : pass);
    }

    public String host() {
        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                LoggerFactory.getLogger(ServerSettings.class)
                             .error(e.getLocalizedMessage(), e);
            }
        }
        return host;
    }

    public String networkName() {
        return networkName;
    }

    public String description() {
        return description;
    }

    public Admin admin() {
        return admin;
    }

    public List<Operator> operators() {
        return operators;
    }

    public List<String> unsafeNickname() {
        return unsafeNickname;
    }

    public List<String> safeNet() {
        return safeNet;
    }

    public String[] motd() {
        return motd;
    }

    @Override
    public String toString() {
        return "ServerSettings{" +
               "port=" + port +
               ", pingTimeout=" + pingTimeout +
               ", timeout=" + timeout +
               ", pass='" + pass + '\'' +
               ", host='" + host + '\'' +
               ", networkName='" + networkName + '\'' +
               ", description='" + description + '\'' +
               ", admin=" + admin +
               ", operators=" + operators +
               ", unsafeNickname=" + unsafeNickname +
               ", safeNet=" + safeNet +
               ", motd=" + Arrays.toString(motd) +
               '}';
    }

    public static class Operator {
        private String username;
        private String host;
        private String password;

        public Operator() {
        }

        public Operator(String username, String host, String password) {
            this.username = username;
            this.host     = host;
            this.password = password;
        }

        public String username() {
            return username;
        }

        public Mask host() {
            return new Mask(host);
        }

        public String password() {
            return password;
        }

        @Override
        public String toString() {
            return "%s@%s {pass=%s}".formatted(username, host, password);
        }
    }

    public static class Admin {
        private String loc1;
        private String loc2;
        private String email;

        public Admin() {
        }

        public Admin(String loc1, String loc2, String email) {
            this.loc1  = loc1;
            this.loc2  = loc2;
            this.email = email;
        }

        public String loc1() {
            return loc1;
        }

        public String loc2() {
            return loc2;
        }

        public String email() {
            return email;
        }

        @Override
        public String toString() {
            return "%s, %s, %s".formatted(loc1, loc2, email);
        }
    }

    public static class Builder {
        private Path                          motdFile       = Path.of("motd.txt");
        private int                           port           = 6667;
        private long                          pingTimeout    = 30000;
        private long                          timeout        = 5000;
        private String                        pass           = "";
        private String                        networkName    = "jircd";
        private String                        description    = "jircd is a lightweight IRC server written in Java.";
        private ServerSettings.Admin          admin          = new ServerSettings.Admin(
                "Location of the server",
                "details of the institution hosting it",
                "admin@jircd.local"
        );
        private List<ServerSettings.Operator> operators      = new ArrayList<>(List.of(
                new ServerSettings.Operator("oper", "*", "oper")
        ));
        private List<String>                  unsafeNickname = new ArrayList<>(List.of(
                // RFC
                "anonymous",
                // anope services
                "ChanServ",
                "NickServ",
                "OperServ",
                "MemoServ",
                "HostServ",
                "BotServ"
        ));
        private List<String>                  safeNet        = new ArrayList<>(
                List.of("::1", "127.0.0.1", "localhost"));
        private String                        host;
        private String[]                      motd           = new String[0];

        public Builder motdFile(Path motdFile) {
            this.motdFile = motdFile;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder pingTimeout(long pingTimeout) {
            this.pingTimeout = pingTimeout;
            return this;
        }

        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder pass(String pass) {
            this.pass = pass;
            return this;
        }

        public Builder networkName(String networkName) {
            this.networkName = networkName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder admin(ServerSettings.Admin admin) {
            this.admin = admin;
            return this;
        }

        public Builder operators(Operator... operators) {
            this.operators = new ArrayList<>(Arrays.asList(operators));
            return this;
        }

        public Builder operators(List<Operator> operators) {
            this.operators = operators;
            return this;
        }

        public Builder unsafeNickname(List<String> unsafeNickname) {
            this.unsafeNickname = unsafeNickname;
            return this;
        }

        public Builder safeNet(List<String> safeNet) {
            this.safeNet = safeNet;
            return this;
        }

        public Builder host(@Nullable String host) {
            this.host = host;
            return this;
        }

        public Builder motd(@Nullable String[] motd) {
            this.motd = motd;
            return this;
        }

        public ServerSettings build() {
            if (motd == null || motd.length == 0) {
                try {
                    motd = Files.exists(motdFile) ? Files.readAllLines(motdFile).toArray(String[]::new) : new String[0];
                } catch (IOException e) {
                    LoggerFactory.getLogger(ServerSettings.class)
                                 .error(e.getLocalizedMessage(), e);
                }
            }
            return new ServerSettings(this);
        }
    }
}

package fr.enimaloc.jircd.server;

import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;
import fr.enimaloc.jircd.message.Mask;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerSettings {

    private transient final static Logger logger = LoggerFactory.getLogger(ServerSettings.class);

    public           int            port           = 6667;
    public           long           pingTimeout    = 30000;
    public           long           timeout        = 5000;
    public           String         pass           = "";
    public           String         host           = "jircd";
    public           String         networkName    = "jircd";
    public           Admin          admin          = new Admin(
            "Location of the server",
            "details of the institution hosting it",
            "administrators@jircd.local"
    );
    public           List<Operator> operators      = new ArrayList<>(List.of(
            new Operator("oper", "*", "oper")
    ));
    public           List<String>   unsafeNickname = new ArrayList<>(List.of(
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
    public           List<String>   safeNet        = new ArrayList<>(List.of("::1", "127.0.0.1", "localhost"));
    public transient String[]       motd;

    public ServerSettings() {
        this(Path.of("motd.txt"));
    }

    public ServerSettings(Path motdFile) {
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        try {
            motd = Files.exists(motdFile) ? Files.readAllLines(motdFile).toArray(String[]::new) : new String[0];
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
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
                  field.setAccessible(true);
                  try {
                      field.set(to, field.get(this));
                  } catch (IllegalAccessException e) {
                      logger.error(e.getLocalizedMessage(), e);
                  }
              });
        return to;
    }

    public void saveAs(Path path) {
        FileConfig settings = FileConfig.of(path);
        new ObjectConverter().toConfig(this, settings);
        settings.save();
        settings.close();
    }

    public void reload(Path path) {
        if (Files.exists(path)) {
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
               && Objects.equals(host, that.host)
               && Objects.equals(networkName, that.networkName)
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
        result = 31 * result + (pass != null ? pass.hashCode() : 0);
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (networkName != null ? networkName.hashCode() : 0);
        result = 31 * result + (admin != null ? admin.hashCode() : 0);
        result = 31 * result + (operators != null ? operators.hashCode() : 0);
        result = 31 * result + (unsafeNickname != null ? unsafeNickname.hashCode() : 0);
        result = 31 * result + (safeNet != null ? safeNet.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(motd);
        return result;
    }

    public static class Operator {
        private String username;
        private String host;
        private String password;

        public Operator() {
        }

        public Operator(String username, String host, String password) {
            this.username = username;
            this.host = host;
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
    }
}

package com.github.enimaloc.irc.jircd.api;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;

public class ServerSettings {

    public        int            port        = 6667;
    public        long           pingTimeout = 100000000;
    public        long           timeout     = 500000000;
    public        String         pass        = "hello";
    public        String         host;
    public        String         networkName = "enimaloc's";
    public        Admin          admin       = new Admin("", "", "");
    public        List<Operator> operators   = new ArrayList<>(
            Collections.singletonList(new Operator("oper", "operPass")));
    private final String[]       defaultMotd = new String[]{
            "This is the default MOTD",
            "You can edit it by creating an motd.txt file",
            "At the root of the directory ans set your motd in !"
    };
    public        String[]       motd;

    public ServerSettings() {
        this(new File("motd.txt"));
    }

    public ServerSettings(File motdFile) {
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            motd = motdFile.exists() ? Files.readAllLines(motdFile.toPath()).toArray(String[]::new) : new String[0];
        } catch (IOException e) {
            e.printStackTrace();
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
              .filter(copyIf)
              .forEach(field -> {
                  field.setAccessible(true);
                  try {
                      field.set(to, field.get(this));
                  } catch (IllegalAccessException e) {
                      e.printStackTrace();
                  }
              });
        return to;
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
        return port == that.port && pingTimeout == that.pingTimeout && timeout == that.timeout && Objects.equals(
                pass, that.pass) && host.equals(that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, pingTimeout, timeout, pass, host);
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
               ", operators=" + operators +
               ", motd=" + Arrays.toString(motd) +
               '}';
    }

    public class Operator {
        private final String username;
        private final String password;

        private Operator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String username() {
            return username;
        }

        public String password() {
            return password;
        }
    }

    public class Admin {
        private final String loc1;
        private final String loc2;
        private final String email;

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

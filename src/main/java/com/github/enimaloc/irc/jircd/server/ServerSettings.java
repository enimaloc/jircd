package com.github.enimaloc.irc.jircd.server;

import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;

public class ServerSettings {

    public int            port        = 6667;
    public long           pingTimeout = 30000;
    public long           timeout     = 5000;
    public String         pass        = "";
    public String         host        = "jircd";
    public String         networkName = "jircd";
    public Admin          admin       = new Admin("", "", "");
    public List<Operator> operators   = new ArrayList<>(List.of(
            new Operator("oper", "oper")
    ));
    public transient String[]       motd;

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

    public void saveAs(File file) {
        FileConfig settings = FileConfig.of(file);
        new ObjectConverter().toConfig(this, settings);
        settings.save();
        settings.close();
    }

    public static class Operator {
        private String username;
        private String password;

        public Operator() {
        }

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
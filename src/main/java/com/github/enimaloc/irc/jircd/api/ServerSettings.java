package com.github.enimaloc.irc.jircd.api;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ServerSettings {

    public        int            port        = 6667;
    public        long           pingTimeout = 100000000;
    public        long           timeout     = 500000000;
    public        String         pass        = "hello";
    public        String         host;
    public        String         networkName = "enimaloc's";
    public        List<Operator> operators   = new ArrayList<>(
            Collections.singletonList(new Operator("oper", "operPass")));
    private final String[]       defaultMotd = new String[]{
            "This is the default MOTD",
            "You can edit it by creating an motd.txt file",
            "At the root of the directory ans set your motd in !"
    };
    public        String[]       motd;

    public ServerSettings() {
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            File motdFile = new File("motd.txt");
            motd = motdFile.exists() ? Files.readAllLines(motdFile.toPath()).toArray(String[]::new) : defaultMotd;
        } catch (IOException e) {
            e.printStackTrace();
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
}

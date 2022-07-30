package fr.enimaloc.jircd.user;

import fr.enimaloc.jircd.server.ServerSettings;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class UserInfo {

    private final User                    user;
    private final long                    joinAt = System.currentTimeMillis();
    private final ServerSettings          settings;
    private       String                  host;
    private       String                  username;
    private       String                  nickname;
    private       String                  realName;
    private       boolean                 passwordValid;
    private       ServerSettings.Operator oper;
    private       List<String>            capabilities; // TODO: 29/07/2022 - Implement CAP command

    public UserInfo(User user, ServerSettings settings) {
        this.user          = user;
        this.passwordValid = !hasString(settings.pass);
        this.settings      = settings;
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

    public boolean canRegistrationBeComplete() {
        return hasString(host) && hasString(username) && hasString(nickname) && hasString(realName) &&
               passwordValid && user.state() != UserState.LOGGED;
    }

    public ServerSettings.Operator oper() {
        return oper;
    }

    public Set<String> capabilities() {
        return capabilities == null ? Set.of() : Set.copyOf(capabilities);
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
        this.user.modes().oper(oper != null);
    }

    public String format() {
        return (hasString(nickname) ? nickname : "@" + host);
    }

    public String full() {
        return nickname + "!" + username + "@" + host;
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
        UserInfo info = (UserInfo) o;
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

    public long joinedAt() {
        return joinAt;
    }

    public boolean secure() {
        return host().matches(String.join("|", settings.safeNet)) || isFromTor();
    }

    public boolean isFromTor() {
        if (!host().matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
            return false;
        }
        String[] split = host().split("\\.");
        try {
            return InetAddress.getByName(
                                      split[3] + "." + split[2] + "." + split[1] + "." + split[0] + ".dnsel.torproject.org")
                              .getHostAddress()
                              .equals("127.0.0.2");
        } catch (UnknownHostException e) {
            return false;
        }
    }
}

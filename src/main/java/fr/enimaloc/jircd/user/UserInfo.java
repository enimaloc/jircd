package fr.enimaloc.jircd.user;

import fr.enimaloc.jircd.server.ServerSettings;
import java.util.Objects;

public class UserInfo {

    private final User                    user;
    private       String                  host;
    private       String                  username;
    private       String                  nickname;
    private       String                  realName;
    private boolean                 passwordValid;
    private ServerSettings.Operator oper;

    public UserInfo(User user, ServerSettings settings) {
        this.user          = user;
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

    public boolean canRegistrationBeComplete() {
        return hasString(host) && hasString(username) && hasString(nickname) && hasString(realName) &&
               passwordValid && user.state() != UserState.LOGGED;
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
}

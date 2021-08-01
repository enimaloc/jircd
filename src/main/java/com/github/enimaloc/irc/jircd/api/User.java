package com.github.enimaloc.irc.jircd.api;

import com.github.enimaloc.irc.jircd.internal.UserImpl;
import com.github.enimaloc.irc.jircd.internal.UserState;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface User {
    void send(Message message);

    void send(String message);

    void terminate(String reason);

    UserImpl.Info info();

    UserState state();

    JIRCD server();

    List<Channel> channels();

    Modes modes();

    Optional<String> away();

    class Info {

        private final User                    user;
        private       String                  host;
        private       String                  username;
        private       String                  nickname;
        private       String                  realName;
        private       boolean                 passwordValid;
        private       ServerSettings.Operator oper;

        public Info(User user, ServerSettings settings) {
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

    class Modes {

        private boolean invisible;
        private boolean oper;
        private boolean localOper;
        private boolean wallops;

        public boolean invisible() {
            return invisible;
        }

        public Modes invisible(boolean invisible) {
            this.invisible = invisible;
            return this;
        }

        public boolean oper() {
            return oper;
        }

        public Modes oper(boolean oper) {
            this.oper = oper;
            return this;
        }

        public boolean localOper() {
            return localOper;
        }

        public Modes localOper(boolean localOper) {
            this.localOper = localOper;
            return this;
        }

        public boolean wallops() {
            return wallops;
        }

        public Modes wallops(boolean wallops) {
            this.wallops = wallops;
            return this;
        }

        @Override
        public String toString() {
            return "Modes{" +
                   "invisible=" + invisible +
                   ", oper=" + oper +
                   ", localOper=" + localOper +
                   ", wallops=" + wallops +
                   '}';
        }

        public String modes() {
            return (invisible() ? "i" : "") +
                   (oper() ? "o" : "") +
                   (localOper() ? "O" : "") +
//                   (registered() ? "r" : "")+
                   (wallops() ? "w" : "");
        }

        public String prefix() {
            return (localOper() || oper() ? "@" : "");
        }
    }
}

package com.github.enimaloc.irc.jircd.internal.exception;

import com.github.enimaloc.irc.jircd.api.ServerSettings;
import com.github.enimaloc.irc.jircd.internal.UserImpl;

public abstract class IRCException extends RuntimeException {
    private final ServerSettings serverSettings;
    private final UserImpl.Info  userInfo;

    public IRCException(ServerSettings serverSettings, UserImpl.Info userInfo) {
        this.serverSettings = serverSettings;
        this.userInfo       = userInfo;
    }

    public IRCException(
            String message, ServerSettings serverSettings, UserImpl.Info userInfo
    ) {
        super(message);
        this.serverSettings = serverSettings;
        this.userInfo       = userInfo;
    }

    public IRCException(
            String message, Throwable cause, ServerSettings serverSettings,
            UserImpl.Info userInfo
    ) {
        super(message, cause);
        this.serverSettings = serverSettings;
        this.userInfo       = userInfo;
    }

    public IRCException(
            Throwable cause, ServerSettings serverSettings, UserImpl.Info userInfo
    ) {
        super(cause);
        this.serverSettings = serverSettings;
        this.userInfo       = userInfo;
    }

    public IRCException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
            ServerSettings serverSettings, UserImpl.Info userInfo
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.serverSettings = serverSettings;
        this.userInfo       = userInfo;
    }

    public abstract String getFormat();

    public String format() {
        return ":" + serverSettings.host + " " + getCode() + " " + userInfo.format() + " " + getFormat();
    }

    public abstract int getCode();

    public UserImpl.Info getUserInfo() {
        return userInfo;
    }

    public static class UnknownError extends IRCException {
        private final String command;
        private final String subCommand;
        private final String info;

        public UnknownError(
                ServerSettings serverSettings, UserImpl.Info userInfo, String command, String subCommand, String info
        ) {
            super(serverSettings, userInfo);
            this.command = command;
            this.subCommand = subCommand;
            this.info = info;
        }

        @Override
        public int getCode() {
            return 400;
        }

        @Override
        public String getFormat() {
            return command +
                   (subCommand != null && !subCommand.isEmpty() && !subCommand.isBlank() ? " " + subCommand : "") +
                   " :" + info;
        }
    }

    public static class NeedMoreParamsError extends IRCException {
        private final String command;

        public NeedMoreParamsError(ServerSettings serverSettings, UserImpl.Info userInfo, String command) {
            super(serverSettings, userInfo);
            this.command = command;
        }

        @Override
        public int getCode() {
            return 461;
        }

        @Override
        public String getFormat() {
            return command + " :Not enough parameters";
        }
    }

    public static class AlreadyRegisteredError extends IRCException {

        public AlreadyRegisteredError(ServerSettings serverSettings, UserImpl.Info userInfo) {
            super(serverSettings, userInfo);
        }

        @Override
        public int getCode() {
            return 462;
        }

        @Override
        public String getFormat() {
            return ":You may not reregister";
        }
    }

    public static class PasswdMismatch extends IRCException {

        public PasswdMismatch(ServerSettings serverSettings, UserImpl.Info userInfo) {
            super(serverSettings, userInfo);
        }

        @Override
        public int getCode() {
            return 464;
        }

        @Override
        public String getFormat() {
            return ":Password incorrect";
        }
    }

    public static class ErroneusNickname extends IRCException {
        private final String nickname;

        public ErroneusNickname(ServerSettings settings, UserImpl.Info info, String nickname) {
            super(settings, info);
            this.nickname = nickname;
        }

        @Override
        public int getCode() {
            return 432;
        }

        @Override
        public String getFormat() {
            return nickname + " :Erroneus nickname";
        }
    }

    public static class NicknameInUse extends IRCException {
        private final String nickname;

        public NicknameInUse(ServerSettings serverSettings, UserImpl.Info userInfo, String nickname) {
            super(serverSettings, userInfo);
            this.nickname = nickname;
        }

        @Override
        public int getCode() {
            return 433;
        }

        @Override
        public String getFormat() {
            return nickname + " :Nickname is already in use";
        }
    }

    public static class NoSuchChannel extends IRCException {
        private final String channel;

        public NoSuchChannel(ServerSettings serverSettings, UserImpl.Info userInfo, String channel) {
            super(serverSettings, userInfo);
            this.channel = channel;
        }

        @Override
        public int getCode() {
            return 403;
        }

        @Override
        public String getFormat() {
            return channel + " :No such channel";
        }
    }

    public static class TooManyChannels extends IRCException {
        private final String channel;

        public TooManyChannels(ServerSettings serverSettings, UserImpl.Info userInfo, String channel) {
            super(serverSettings, userInfo);
            this.channel = channel;
        }

        @Override
        public int getCode() {
            return 405;
        }

        @Override
        public String getFormat() {
            return channel + " :You have joined too many channels";
        }
    }

    public static class BadChannelKey extends IRCException {
        private final String channel;

        public BadChannelKey(ServerSettings serverSettings, UserImpl.Info userInfo, String channel) {
            super(serverSettings, userInfo);
            this.channel = channel;
        }

        @Override
        public int getCode() {
            return 475;
        }

        @Override
        public String getFormat() {
            return channel + " :Cannot join channel (+k)";
        }
    }

    public static class BannedFromChan extends IRCException {
        private final String channel;

        public BannedFromChan(ServerSettings serverSettings, UserImpl.Info userInfo, String channel) {
            super(serverSettings, userInfo);
            this.channel = channel;
        }

        @Override
        public int getCode() {
            return 474;
        }

        @Override
        public String getFormat() {
            return channel + " :Cannot join channel (+b)";
        }
    }

    public static class ChannelIsFull extends IRCException {
        private final String channel;

        public ChannelIsFull(ServerSettings serverSettings, UserImpl.Info userInfo, String channel) {
            super(serverSettings, userInfo);
            this.channel = channel;
        }

        @Override
        public int getCode() {
            return 471;
        }

        @Override
        public String getFormat() {
            return channel + " :Cannot join channel (+l)";
        }
    }

    public static class InviteOnlyChan extends IRCException {
        private final String channel;

        public InviteOnlyChan(ServerSettings serverSettings, UserImpl.Info userInfo, String channel) {
            super(serverSettings, userInfo);
            this.channel = channel;
        }

        @Override
        public int getCode() {
            return 473;
        }

        @Override
        public String getFormat() {
            return channel + " :Cannot join channel (+i)";
        }
    }

    public static class NotOnChannel extends IRCException {
        private final String channel;

        public NotOnChannel(ServerSettings serverSettings, UserImpl.Info userInfo, String channel) {
            super(serverSettings, userInfo);
            this.channel = channel;
        }

        @Override
        public int getCode() {
            return 442;
        }

        @Override
        public String getFormat() {
            return channel + " :You're not on that channel";
        }
    }
}

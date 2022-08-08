package fr.enimaloc.jircd.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class ChannelModes {
    private final List<String> except;
    private final List<String> bans;
    private final List<String> invEx;
    private       String       password;
    private       int          limit;
    private       boolean      moderate;
    private       boolean      inviteOnly;
    private       boolean      secret;
    private       boolean      _protected;
    private       boolean      noExternalMessage;

    public ChannelModes(
            List<String> except, List<String> bans, List<String> invEx, String password, int limit,
            boolean moderate,
            boolean inviteOnly,
            boolean secret,
            boolean _protected,
            boolean noExternalMessage
    ) {
        this.except            = except == null ? new ArrayList<>() : except;
        this.bans              = bans == null ? new ArrayList<>() : bans;
        this.invEx             = invEx == null ? new ArrayList<>() : invEx;
        this.password          = password;
        this.limit             = limit;
        this.moderate          = moderate;
        this.inviteOnly        = inviteOnly;
        this.secret            = secret;
        this._protected        = _protected;
        this.noExternalMessage = noExternalMessage;
    }

    public Optional<String> password() {
        return Optional.ofNullable(password);
    }

    public OptionalInt limit() {
        return limit < 1 ? OptionalInt.empty() : OptionalInt.of(limit);
    }

    public boolean inviteOnly() {
        return inviteOnly;
    }

    public boolean secret() {
        return secret;
    }

    public void password(String password) {
        this.password = password;
    }

    public void limit(int limit) {
        this.limit = limit;
    }

    public void inviteOnly(boolean inviteOnly) {
        this.inviteOnly = inviteOnly;
    }

    public void secret(boolean secret) {
        this.secret = secret;
    }

    public List<String> bans() {
        return bans;
    }

    public boolean moderate() {
        return moderate;
    }

    public ChannelModes moderate(boolean moderate) {
        this.moderate = moderate;
        return this;
    }

    public boolean _protected() {
        return _protected;
    }

    public ChannelModes _protected(boolean _protected) {
        this._protected = _protected;
        return this;
    }

    public boolean noExternalMessage() {
        return noExternalMessage;
    }

    public ChannelModes noExternalMessage(boolean noExternalMessage) {
        this.noExternalMessage = noExternalMessage;
        return this;
    }

    public List<String> except() {
        return except;
    }

    public List<String> invEx() {
        return invEx;
    }

    public String modesString() {
        return
                (!bans().isEmpty() ? "b" : "") +
                (!except().isEmpty() ? "e" : "") +
                (inviteOnly() ? "i" : "") +
                (!invEx().isEmpty() ? "I" : "") +
                (moderate() ? "m" : "") +
                (_protected() ? "t" : "") +
                (noExternalMessage() ? "n" : "") +
                (secret() ? "s" : "") +
                (limit().isPresent() ? "l" : "") +
                (password().isPresent() ? "k" : "");
    }

    public String modesArguments() {
        return (limit().orElse(-1) + " " + password().orElse(""))
                .replaceFirst("-1 ", "") // No limit
                .replaceAll(" $", ""); // No password
    }
}

package fr.enimaloc.jircd.channel;

import fr.enimaloc.enutils.classes.NumberUtils;
import java.util.*;
import java.util.function.Predicate;
import org.jetbrains.annotations.Range;

public final class ChannelModes {
    private List<String> except;
    private List<String> bans;
    private List<String> invEx;
    private String       password;
    private int          limit;
    private boolean      moderate;
    private boolean      inviteOnly;
    private boolean secret;
    private boolean protected0;
    private boolean noExternalMessage;

    public ChannelModes() {
        this.except            = new ArrayList<>();
        this.bans              = new ArrayList<>();
        this.invEx             = new ArrayList<>();
        this.password          = null;
        this.limit             = 0;
        this.moderate          = false;
        this.inviteOnly        = false;
        this.secret            = false;
        this.protected0        = false;
        this.noExternalMessage = false;
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

    public ChannelModes password(String password) {
        this.password = password == null || password.isEmpty() || password.isBlank() ? null : password;
        return this;
    }

    public ChannelModes limit(@Range(from = 0, to = Integer.MAX_VALUE) int limit) {
        this.limit = limit;
        return this;
    }

    public ChannelModes inviteOnly(boolean inviteOnly) {
        this.inviteOnly = inviteOnly;
        return this;
    }

    public ChannelModes secret(boolean secret) {
        this.secret = secret;
        return this;
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

    public boolean protected0() {
        return protected0;
    }

    public ChannelModes protected0(boolean protected0) {
        this.protected0 = protected0;
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
                (protected0() ? "t" : "") +
                (noExternalMessage() ? "n" : "") +
                (secret() ? "s" : "") +
                (limit().isPresent() ? "l" : "") +
                (password().isPresent() ? "k" : "");
    }

    public Map<Character, Boolean> apply(String rawMode, String arg, Map<Character, Predicate<Void>> onChar) {
        Map<Character, Boolean> result = new HashMap<>();
        var ref = new Object() {
            boolean add = true;
            boolean break0 = false;

            final List<String> except = ChannelModes.this.except;
            final List<String> bans = ChannelModes.this.bans;
            final List<String> invEx = ChannelModes.this.invEx;
            String password = ChannelModes.this.password;
            int limit = ChannelModes.this.limit;
            boolean moderate = ChannelModes.this.moderate;
            boolean inviteOnly = ChannelModes.this.inviteOnly;
            boolean secret = ChannelModes.this.secret;
            boolean protected0 = ChannelModes.this.protected0;
            boolean noExternalMessage = ChannelModes.this.noExternalMessage;
        };
        for (char mode : rawMode.toCharArray()) {
            if (ref.break0) {
                return Collections.emptyMap();
            }
            switch (mode) {
                case '+', '-' -> ref.add = mode == '+';
                case 'b' -> and(() -> result.put(mode, ref.add),
                                () -> boolAction(ref.add, ref.bans, arg),
                                () -> ref.break0 = !onChar.getOrDefault(mode, unused -> true).test(null));
                case 'e' -> and(() -> result.put(mode, ref.add),
                                () -> boolAction(ref.add, ref.except, arg),
                                () -> ref.break0 = !onChar.getOrDefault(mode, unused -> true).test(null));
                case 'I' -> and(() -> result.put(mode, ref.add),
                                () -> boolAction(ref.add, ref.invEx, arg),
                                () -> ref.break0 = !onChar.getOrDefault(mode, unused -> true).test(null));
                case 'l' -> and(() -> result.put(mode, ref.add),
                                () -> ref.limit = NumberUtils.getSafe(ref.add ? arg : "0", Integer.class).orElse(0),
                                () -> ref.break0 = !onChar.getOrDefault(mode, unused -> true).test(null));
                case 'i' -> and(() -> result.put(mode, ref.add),
                                () -> ref.inviteOnly = ref.add,
                                () -> ref.break0 = !onChar.getOrDefault(mode, unused -> true).test(null));
                case 'k' -> and(() -> result.put(mode, ref.add),
                                () -> ref.password = ref.add ? arg : null,
                                () -> ref.break0 = !onChar.getOrDefault(mode, unused -> true).test(null));
                case 'm' -> and(() -> result.put(mode, ref.add),
                                () -> ref.moderate = ref.add,
                                () -> ref.break0 = !onChar.getOrDefault(mode, unused -> true).test(null));
                case 's' -> and(() -> result.put(mode, ref.add),
                                () -> ref.secret = ref.add,
                                () -> ref.break0 = !onChar.getOrDefault(mode, unused -> true).test(null));
                case 't' -> and(() -> result.put(mode, ref.add),
                                () -> ref.protected0 = ref.add,
                                () -> ref.break0 = !onChar.getOrDefault(mode, unused -> true).test(null));
                case 'n' -> and(() -> result.put(mode, ref.add),
                                () -> ref.noExternalMessage = ref.add,
                                () -> ref.break0 = !onChar.getOrDefault(mode, unused -> true).test(null));
                default -> ref.break0 = !onChar.getOrDefault(null, unused -> true).test(null);
            }
        }
        this.bans              = ref.bans;
        this.except            = ref.except;
        this.invEx             = ref.invEx;
        this.password          = ref.password;
        this.limit             = ref.limit;
        this.moderate          = ref.moderate;
        this.inviteOnly        = ref.inviteOnly;
        this.secret            = ref.secret;
        this.protected0        = ref.protected0;
        this.noExternalMessage = ref.noExternalMessage;
        return result;
    }

    private void and(Runnable... runnables) {
        for (Runnable runnable : runnables) {
            runnable.run();
        }
    }

    private <T> void boolAction(boolean add, List<T> l, T t) {
        if (add) {
            l.add(t);
        } else {
            l.remove(t);
        }
    }

    public String modesArguments() {
        return (limit().orElse(-1) + " " + password().orElse(""))
                .replaceFirst("-1 ", "") // No limit
                .replaceAll(" $", ""); // No password
    }
}

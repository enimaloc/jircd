package fr.enimaloc.jircd.message;

import java.util.regex.Pattern;

public class Regex {

    Regex() {}

    public static final Pattern NICKNAME       = Pattern.compile("[a-zA-Z][a-zA-Z0-9\\-_]{0,15}");
    public static final Pattern CHANNEL        = Pattern.compile("&|#[a-zA-Z0-9\\-_]{1,49}");
    public static final Pattern CHANNEL_PREFIX = Pattern.compile("[~&@%+]");
}

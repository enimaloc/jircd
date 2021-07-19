package com.github.enimaloc.irc.jircd.internal;

import java.util.regex.Pattern;

public class Regex {

    public static final Pattern NICKNAME = Pattern.compile("[\\[\\]\\\\`_^{|}A-z][\\[\\]\\\\`_\\^{\\|}A-z0-9\\-]{0,7}");

}

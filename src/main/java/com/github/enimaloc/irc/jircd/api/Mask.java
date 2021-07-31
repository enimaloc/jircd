package com.github.enimaloc.irc.jircd.api;

import java.util.regex.Pattern;

public class Mask {

    private final String mask;

    public Mask(String mask) {
        this.mask = mask;
    }

    public String toRegex() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < mask.toCharArray().length; i++) {
            char c = mask.toCharArray()[i];
            switch (c) {
                case '\\' -> ret.append(mask.toCharArray()[i++]);
                case '?' -> ret.append(".");
                case '*' -> ret.append(".*");
                default -> ret.append(c);
            }
        }
        return ret.toString();
    }

    public Pattern toPattern() {
        return Pattern.compile(toRegex());
    }
}

package fr.enimaloc.jircd.message;

import java.util.regex.Pattern;

public record Mask(String mask) {

    public String toRegex() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < mask.length(); i++) {
            char c = mask.charAt(i);
            switch (c) {
                case '\\' -> ret.append(Pattern.quote(mask.charAt(++i)+""));
                case '?' -> ret.append(".");
                case '*' -> ret.append(".*");
                default -> ret.append(Pattern.quote(c+""));
            }
        }
        return ret.toString();
    }

    public Pattern toPattern() {
        return Pattern.compile(toRegex());
    }
}

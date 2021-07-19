package com.github.enimaloc.utils;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;

public class Utils {
    public static Optional<String> getGroup(Matcher matcher, String groupName) {
        return Optional.ofNullable(getOr(() -> matcher.group(groupName), null));
    }

    public static <T> T getOr(Supplier<T> supplier, T or) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return or;
        }
    }
}

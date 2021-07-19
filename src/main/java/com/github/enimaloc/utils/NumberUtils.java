package com.github.enimaloc.utils;

import java.util.Optional;

public class NumberUtils {
    /**
     * Converts a {@link String} into its numerical value. If the provided {@link String} does not represent a number,
     * an empty {@link Optional} is returned
     *
     * @param value       {@link String} to parse
     * @param targetClazz used to define the return type
     * @param <T>         target class defined with {@code value} parameter, need to {@code extends} {@link Number} {@link Class}
     * @return an {@link Optional} which can be empty if the {@link String value} cannot be parsed in
     * the target {@link Number class} else the {@link String value} parsed in the {@link Number targeted}
     * {@link Class}
     */
    public static <T extends Number> Optional<T> getSafe(String value, Class<T> targetClazz) {
        try {
            T target;
            if (Byte.class == targetClazz) {
                target = (T) Byte.valueOf(value);
            } else if (Double.class == targetClazz) {
                target = (T) Double.valueOf(value);
            } else if (Float.class == targetClazz) {
                target = (T) Float.valueOf(value);
            } else if (Integer.class == targetClazz) {
                target = (T) Integer.valueOf(value);
            } else if (Long.class == targetClazz) {
                target = (T) Long.valueOf(value);
            } else if (Short.class == targetClazz) {
                target = (T) Short.valueOf(value);
            } else {
                throw new IllegalArgumentException(
                        String.format("Class %s is not currently supported", targetClazz.getName()));
            }
            return Optional.of(target);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}

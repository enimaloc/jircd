/*
 * TriConsumer
 *
 * 0.0.1
 *
 * 15/08/2022
 */
package fr.enimaloc.jircd.utils.function;

import java.util.Objects;

/**
 *
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    void accept(T t, U u, V v);

    default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);

        return (l, c, r) -> {
            accept(l, c, r);
            after.accept(l, c, r);
        };
    }
}

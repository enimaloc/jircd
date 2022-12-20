/*
 * CommandTest
 *
 * 0.0.1
 *
 * 08/08/2022
 */
package utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.DisplayNameGeneration;

/**
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@DisplayNameGeneration(CommandNameGen.class)
public @interface CommandClazzTest {
}

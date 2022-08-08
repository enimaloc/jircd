package fr.enimaloc.jircd.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Command {

    String   DEFAULT_STRING     = "\0";

    String name() default DEFAULT_STRING;

    boolean trailing() default false;

    record CommandIdentifier(int parametersCount, boolean hasTrailing) {}

    record CommandIdentity(Object instance, Method method) {}
}

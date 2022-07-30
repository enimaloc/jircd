package fr.enimaloc.jircd.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Command {

    String name() default "__DEFAULT__";

    boolean trailing() default false;

    record CommandIdentifier(int parametersCount, boolean hasTrailing) {}

    record CommandIdentity(Object instance, Method method) {}
}

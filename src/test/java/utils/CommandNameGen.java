/*
 * CommandNameTest
 *
 * 0.0.1
 *
 * 08/08/2022
 */
package utils;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayNameGenerator;

/**
 *
 */
public class CommandNameGen implements DisplayNameGenerator {
    @Override
    public String generateDisplayNameForClass(Class<?> testClass) {
        return testClass.getSimpleName().endsWith("CommandTest")
                ? testClass.getSimpleName().substring(0, testClass.getSimpleName().length() - "CommandTest".length()).toUpperCase()
                : testClass.getSimpleName();
    }

    @Override
    public String generateDisplayNameForNestedClass(Class<?> nestedClass) {
        return generateDisplayNameForClass(nestedClass);
    }

    @Override
    public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
        Class<?> clazz = testClass;
        while (clazz != clazz.getNestHost()) {
            clazz = clazz.getNestHost();
        }
        String clazzName = generateDisplayNameForClass(clazz);
        String methodName = testMethod.getName();
        if (methodName.toLowerCase().startsWith(clazzName.toLowerCase())) {
            methodName = methodName.substring(clazzName.length());
        }
        if (methodName.toLowerCase().endsWith("test")) {
            methodName = methodName.substring(0, methodName.length() - "test".length());
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < methodName.length(); i++) {
            char c = methodName.charAt(i);
            if ((Character.isUpperCase(c) || Character.isDigit(c)) && i > 0) {
                sb.append(' ');
                c = Character.toLowerCase(c);
            }
            sb.append(c);
        }
        return sb.isEmpty() ? "Default" : sb.toString();
    }
}

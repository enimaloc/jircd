package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.Constant;
import fr.enimaloc.jircd.server.attributes.SupportAttribute;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CommandClazzTest;

import static org.junit.jupiter.api.Assertions.*;

@CommandClazzTest
class VersionCommandTest extends ServerCommandBase {

    @BeforeEach
    void setUp() {
        init();
    }

    @Test
    void versionWithUnknownServerTest() {
        connections[0].send("VERSION UnknownServer");
        assertArrayEquals(new String[]{
                ":jircd-host 402 bob UnknownServer :No such server"
        }, connections[0].awaitMessage());
    }

    @Test
    void versionTest() {
        connections[0].send("VERSION");

        assertArrayEquals(new String[]{
                ":jircd-host 351 bob %s jircd-host :".formatted(Constant.VERSION)
        }, connections[0].awaitMessage());

        SupportAttribute attr = server.supportAttribute();
        for (int i = 0; i < attrLength; i++) {
            String[] messages = connections[0].awaitMessage();
            if (messages.length == 0) {
                continue;
            }
            String isSupport = messages[0];
            assertTrue(isSupport.startsWith(":jircd-host 005 bob "));
            assertTrue(isSupport.endsWith(":are supported by this server"));
            isSupport = isSupport.replaceFirst(":jircd-host 005 bob ", "")
                                 .replace(" :are supported by this server", "");

            String[] attributes = isSupport.split(" ");
            assertTrue(attributes.length <= 13);
            for (String attribute : attributes) {
                String key = attribute.contains("=") ?
                        attribute.split("=")[0] :
                        attribute;
                String value = attribute.contains("=") ?
                        attribute.split("=")[1] :
                        null;
                Map<String, Object> map       = attr.asMap((s, o) -> s.equalsIgnoreCase(key));
                String              fieldName = (String) map.keySet().toArray()[0];
                Object              expectedValue = map.values().toArray()[0];
                Class<?>            expectedClazz = null;
                Class<?>            actualClazz   = null;
                Object              actualValue   = null;
                if (value != null) {
                    try {
                        actualValue = Integer.parseInt(value);
                        actualClazz = Integer.class;
                    } catch (NumberFormatException ignored) {
                        if (value.equals("true") || value.equals("false")) {
                            actualValue = Boolean.parseBoolean(value);
                            actualClazz = Boolean.class;
                        } else {
                            if (value.contains(",") && Arrays.stream(value.split(",")).allMatch(
                                    s -> s.length() == 1)) {
                                actualValue = value.toCharArray();
                                actualClazz = Character[].class;
                            } else {
                                actualValue = value;
                                actualClazz = String.class;
                            }
                        }
                    }
                }
                if (expectedValue instanceof Optional) {
                    try {
                        if (value == null) {
                            actualClazz
                                          = (Class<?>) ((ParameterizedType) SupportAttribute.class.getDeclaredField(
                                    fieldName).getGenericType()).getActualTypeArguments()[0];
                            expectedValue = null;
                        }
                        expectedClazz
                                      = (Class<?>) ((ParameterizedType) SupportAttribute.class.getDeclaredField(
                                fieldName).getGenericType()).getActualTypeArguments()[0];
                        expectedValue = expectedValue != null ?
                                ((Optional<?>) expectedValue).orElse(null) :
                                null;
                    } catch (NoSuchFieldException e) {
                        fail(e);
                    }
                } else if (expectedValue instanceof OptionalInt) {
                    if (value == null) {
                        actualClazz   = Integer.class;
                        expectedValue = null;
                    }
                    expectedClazz = Integer.class;
                    // Is present is not detected by idea here
                    //noinspection OptionalGetWithoutIsPresent
                    expectedValue = expectedValue != null && ((OptionalInt) expectedValue).isPresent() ?
                            ((OptionalInt) expectedValue).getAsInt() :
                            null;
                } else {
                    expectedClazz = expectedValue.getClass();
                }

                assertEquals(expectedValue, actualValue);
                assertEquals(expectedClazz, actualClazz);
            }
        }
    }

    @AfterEach
    void tearDown() {
        off();
    }
}
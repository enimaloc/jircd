package fr.enimaloc.jircd.server.attributes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiPredicate;
import org.slf4j.LoggerFactory;

public class Attribute {

    public List<Map<String, Object>> asMapsWithLimit(int limit) {
        return asMapsWithLimit(limit, null);
    }

    public List<Map<String, Object>> asMapsWithLimit(int limit, BiPredicate<String, Object> filter) {
        List<Map<String, Object>> ret = new ArrayList<>();
        if (asMap(filter).keySet().size() > limit) {
            Map<String, Object> actual = new HashMap<>();
            for (int i = 0; i < asMap(filter).keySet().size(); i++) {
                if (i % limit == 0) {
                    ret.add(actual);
                    actual = new HashMap<>();
                }
                String key = asMap(filter).keySet().toArray(String[]::new)[i];
                actual.put(key, asMap(filter).get(key));
            }
            ret.add(actual);
        } else {
            ret.add(asMap(filter));
        }
        return ret;
    }

    public Map<String, Object> asMap() {
        return asMap(null);
    }

    public Map<String, Object> asMap(BiPredicate<String, Object> filter) {
        Map<String, Object> ret = new HashMap<>();
        for (Field declaredField : Arrays.stream(this.getClass().getDeclaredFields())
                                         .filter(f -> !Modifier.isTransient(f.getModifiers()))
                                         .toArray(Field[]::new)) {
            try {
                if (filter != null && filter.test(declaredField.getName(), declaredField.get(this))) {
                    ret.put(declaredField.getName(), declaredField.get(this));
                }
            } catch (IllegalAccessException e) {
                LoggerFactory.getLogger(Attribute.class).error(e.getLocalizedMessage(), e);
            }
        }
        return ret;
    }

    public int length() {
        return asMap((s, o) -> o != null).size();
    }
}

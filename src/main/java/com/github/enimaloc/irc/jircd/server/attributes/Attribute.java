package com.github.enimaloc.irc.jircd.server.attributes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

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
        for (Field declaredField : this.getClass().getDeclaredFields()) {
            try {
                if (filter != null && filter.test(declaredField.getName(), declaredField.get(this))) {
                    ret.put(declaredField.getName(), declaredField.get(this));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public int length() {
        return asMap((s, o) -> o != null).size();
    }
}

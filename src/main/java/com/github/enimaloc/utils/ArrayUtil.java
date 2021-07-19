package com.github.enimaloc.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ArrayUtil {

    public static <T> T[] removeIf(T[] array, Predicate<T> removeIf) {
        List<T> list = new ArrayList<T>(Arrays.asList(array));
        list.removeIf(removeIf);
        T[] ret = (T[]) Array.newInstance(array.getClass(), list.size());
        for (int i = 0; i < ret.length; i++) {
            ret[i] = list.get(i);
        }
        return ret;
    }

}

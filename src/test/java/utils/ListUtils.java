package utils;

import java.util.List;
import java.util.Random;

public class ListUtils {

    public static <T> T getRandom(List<T> list) {
        return !list.isEmpty() ? list.get(new Random().nextInt(list.size()-1)) : null;
    }

}

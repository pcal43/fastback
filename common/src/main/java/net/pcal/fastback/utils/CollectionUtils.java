package net.pcal.fastback.utils;

import java.util.List;

public class CollectionUtils {
    public static <T> void addIf(List<T> list, boolean expr, T element) {
        if (expr) {
            list.add(element);
        }
    }
}

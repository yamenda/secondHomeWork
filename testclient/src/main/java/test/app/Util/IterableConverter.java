package test.app.Util;

import java.util.ArrayList;
import java.util.List;

public class IterableConverter {
    public static <T> List<T> toList(Iterable<T> iterable) {
        if (iterable instanceof List)
            return (List<T>) iterable;
        List<T> list = new ArrayList<T>();
        if (iterable != null)
            for (T t : iterable)
                list.add(t);
        return list;
    }
}

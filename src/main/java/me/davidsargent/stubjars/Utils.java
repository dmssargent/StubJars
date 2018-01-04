package me.davidsargent.stubjars;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class Utils {
    @NotNull
    public static <T> String arrayToCommaSeparatedList(T[] elements, Function<T, String> func) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < elements.length; ++i) {
            String value = func.apply(elements[i]);
            builder.append(value);
            if (i < elements.length - 1) builder.append(", ");
        }

        return builder.toString();
    }

    @NotNull
    public static <T> String arrayToCommaSeparatedList(T element, int n, Function<T, String> func) {
        return arrayToCommaSeparatedList(func.apply(element), n);
    }

    @NotNull
    private static <T> String arrayToCommaSeparatedList(String element, int n) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < n; ++i) {
            builder.append(element);
            if (i < n - 1) builder.append(", ");
        }

        return builder.toString();
    }
}

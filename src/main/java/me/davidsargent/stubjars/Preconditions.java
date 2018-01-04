package me.davidsargent.stubjars;

import org.jetbrains.annotations.Contract;

public class Preconditions {
    @Contract("null -> fail")
    public static void checkNotNull(Object o) {
        if (o == null)
            throw new NullPointerException();
    }
}

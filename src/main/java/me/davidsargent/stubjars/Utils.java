/*
 *  Copyright 2018 David Sargent
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package me.davidsargent.stubjars;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class Utils {
    @NotNull
    public static <T> String arrayToCommaSeparatedList(T[] elements, Function<T, String> func) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < elements.length; ++i) {
            String value = func.apply(elements[i]);
            if (value == null) continue;
            builder.append(value);
            if (i < elements.length - 1 && !(i == elements.length - 2 && func.apply(elements[i + 1]) == null)) builder.append(", ");
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

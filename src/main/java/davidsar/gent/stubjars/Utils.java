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

package davidsar.gent.stubjars;

import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Function;

public class Utils {
    @NotNull
    @Deprecated
    public static <T> String arrayToCommaSeparatedList(T[] elements, Function<T, String> func) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < elements.length; ++i) {
            String value = func.apply(elements[i]);
            if (value == null) {
                continue;
            }
            builder.append(value);
            if (i < elements.length - 1 && !(i == elements.length - 2 && func.apply(elements[i + 1]) == null)) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    @NotNull
    public static <T> Expression arrayToListExpression(T[] elements, Function<T, Expression> function) {
        return Expressions.makeListFrom(
            Arrays.stream(elements).map(function).toArray(Expression[]::new)
        );
    }
}

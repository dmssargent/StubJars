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

package davidsar.gent.stubjars.components;

import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import davidsar.gent.stubjars.components.expressions.StringExpression;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class Value {
    /**
     * Returns a String contains the default value for a given type.
     *
     * @param type a {@link Type} to get the default value for
     * @return a String with the default type for the parameter type
     */
    static Expression defaultValueForType(Type type, JarClass<?> against) {
        return defaultValueForType(type, against, false);
    }

    /**
     * Returns a String contains the default value for a given type.
     *
     * @param type a {@link Type} to get the default value for
     * @param constant {@code true} if the returned value should be a constant
     * @return a String with the default type for the parameter type
     */
    static Expression defaultValueForType(Type type, JarClass<?> against, boolean constant) {
        if (!(type instanceof Class)) {
            if (type instanceof ParameterizedType) {
                return defaultValueForType(((ParameterizedType) type).getRawType(), against);
            }

            return defaultValueForType(Object.class, against);
        }

        if (type.equals(int.class) || type.equals(Integer.class)) {
            return Expressions.fromString("0");
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return Expressions.fromString("0.0");
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return Expressions.fromString("0L");
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return Expressions.toCast(byte.class, 0);
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            return Expressions.toCast(short.class, 0);
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Expressions.fromString(Boolean.toString(false));
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return Expressions.toCast(float.class, 0);
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            return Expressions.toCast(char.class, 0);
        } else if (type.equals(String.class)) {
            return Expressions.fromString("\"\"");
        } else if (((Class) type).isArray()) {
            if (constant) {
                return Expressions.fromString("{}");
            }

            return Expressions.of(
                Expressions.fromString("new"),
                StringExpression.SPACE,
                JarType.toExpression(type, against),
                StringExpression.SPACE,
                Expressions.emptyBlock());
        } else if (((Class) type).isEnum()) {
            //noinspection unchecked
            Enum[] enumConstants = JarClass.getEnumConstantsFor((Class<? extends Enum>) type);
            if (enumConstants == null) {
                if (constant) {
                    throw new RuntimeException("Cannot determine constant value!");
                }

                return Expressions.fromString("null");
            }

            return Expressions.of(
                JarType.toExpression(type, against),
                StringExpression.PERIOD,
                Expressions.fromString(enumConstants[0].name())
            );
        } else {
            return Expressions.fromString("null");
        }
    }
}

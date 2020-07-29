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
            if (constant) {
                return Expressions.fromString("0");
            }
            return Expressions.fromString("Integer.valueOf(0)");
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            if (constant) {
                return Expressions.fromString("0.0");
            }
            return Expressions.fromString("Double.valueOf('\\0').doubleValue()");
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            if (constant) {
                return Expressions.fromString("0L");
            }
            return Expressions.fromString("Long.valueOf('\\0').longValue()");
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            if (constant) {
                return Expressions.fromString("0");
            }
            return Expressions.toCast(byte.class, "Integer.valueOf(0).byteValue()");
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            if (constant) {
                return Expressions.toCast(short.class, "0");
            }
            return Expressions.toCast(short.class, "Integer.valueOf(0).shortValue()");
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            if (constant) {
                return Expressions.fromString("false");
            }
            return Expressions.fromString("Boolean.valueOf(false).booleanValue()");
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            if (constant) {
                return Expressions.fromString("0.0f");
            }
            return Expressions.toCast(float.class, "Float.valueOf('\\0').floatValue()");
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            if (constant) {
                return Expressions.fromString("'\\0'");
            }
            return Expressions.toCast(char.class, "Character.valueOf('\\0').charValue()");
        } else if (type.equals(String.class)) {
            if (constant) {
                return Expressions.fromString("\"\"");
            }
            return Expressions.fromString("\"\".toString()");
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

    public static String reduceValueToString(Class<?> expectedType, Object o) {
        if (o instanceof String) {
            // Per the Java 3.10.6 Spec
            String filteredString = ((String) o)
                .replaceAll("([\b\t\n\f\r\"\']|\\\\)", "\\$1");
            return "\"" + filteredString + "\"";
        } else if (o instanceof Integer || o instanceof Short || o instanceof Boolean) {
            return String.valueOf(o);
        } else if (o instanceof Double) {
            if (Double.isNaN((Double) o)) {
                return "Double.NaN";
            } else if (Double.isInfinite((Double) o)) {
                if ((double) o < 0) {
                    return "Double.NEGATIVE_INFINITY";
                }

                return "Double.POSITIVE_INFINITY";
            }

            return Double.toString((Double) o);
        } else if (o instanceof Long) {
            return o + "L";
        } else if (o instanceof Float) {
            return o + "F";
        } else if (o instanceof Byte) {
            return Byte.toString((byte) o);
        }

        return "null";
    }
}

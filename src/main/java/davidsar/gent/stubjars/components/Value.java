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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class Value {
    /**
     * Returns a String contains the default value for a given type
     *
     * @param type a {@link Type} to get the default value for
     * @return a String with the default type for the parameter type
     */
    public static String defaultValueForType(Type type) {
        return defaultValueForType(type, false);
    }

    /**
     * Returns a String contains the default value for a given type
     *
     * @param type     a {@link Type} to get the default value for
     * @param constant {@code true} if the returned value should be a constant
     * @return a String with the default type for the parameter type
     */
    public static String defaultValueForType(Type type, boolean constant) {
        if (!(type instanceof Class)) {
            if (type instanceof ParameterizedType) return defaultValueForType(((ParameterizedType) type).getRawType());
            return defaultValueForType(Object.class);
        }

        if (type.equals(int.class) || type.equals(Integer.class)) {
            return "0";
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return "0.0";
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return "0L";
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return Expression.cast(byte.class, 0);
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            return Expression.cast(short.class, 0);
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.toString(false);
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return Expression.cast(float.class, 0);
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            return Expression.cast(char.class, 0);
        } else if (type.equals(String.class)) {
            return "\"\"";
        } else if (((Class) type).isArray()) {
            if (constant)
                return "{}";
            else
                return String.format("new %s {}", JarType.toString(type));
        } else if (((Class) type).isEnum()) {
            //noinspection unchecked
            Enum[] enumConstants = JarClass.getEnumConstantsFor((Class<? extends Enum>) type);
            if (enumConstants == null) {
                if (constant) throw new RuntimeException("Cannot determine constant value!");

                return "null";
            }

            return JarType.toString(type) + "." + enumConstants[0].name();
        } else {
            return "null";
        }
    }
}

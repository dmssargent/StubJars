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

import davidsar.gent.stubjars.Utils;
import davidsar.gent.stubjars.components.writer.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

class JarType {
    private final Type type;

    public JarType(@NotNull Type type) {
        this.type = type;
    }

    static String convertTypeParametersToString(TypeVariable<?>[] typeParameters) {
        String genericS;
        if (typeParameters.length == 0) {
            genericS = Constants.SPACE;
        } else {
            String typeParams = Utils.arrayToCommaSeparatedList(typeParameters, typeParam -> {
                if (typeParam.getBounds()[0] == Object.class) {
                    return typeParam.getName();
                } else {
                    return typeParam.getName() + " extends " + JarType.toString(typeParam.getBounds()[0]);
                }
            });
            genericS = "<" + typeParams + "> ";
        }

        return genericS;
    }

    @NotNull
    @Override
    public String toString() {
        return toString(type);
    }

    @NotNull
    static String toString(@NotNull Type type) {
        return toString(type, false, null);
    }

    @NotNull
    static String toString(@NotNull Type type, boolean keepSimple, @Nullable Function<TypeVariable, String> resolver) {
        if (type instanceof Class) {
            return JarClass.safeFullNameForClass((Class<?>) type);
        }

        if (type instanceof ParameterizedType) {
            StringBuilder builder = new StringBuilder();
            ParameterizedType pType = (ParameterizedType) type;
            if (pType.getOwnerType() != null) {
                Type ownerType = pType.getOwnerType();
                if (ownerType instanceof Class) {
                    builder.append(JarClass.safeFullNameForClass((Class<?>) pType.getRawType()));
                } else if (ownerType instanceof ParameterizedType) {
                    builder.append(toString(ownerType));
                    builder.append(".");
                    Class<?> rawTypeOfOwner = (Class<?>) ((ParameterizedType) pType.getOwnerType()).getRawType();
                    String rawType = ((Class<?>) pType.getRawType()).getName()
                            .replace(rawTypeOfOwner.getName() + "$", Constants.EMPTY_STRING);
                    builder.append(rawType);
                } else {
                    throw new UnsupportedOperationException(type.getClass().getName());
                }
            } else {
                builder.append(JarClass.safeFullNameForClass((Class<?>) pType.getRawType()));
            }

            Type[] actualTypeArguments = pType.getActualTypeArguments();
            if (!keepSimple && (actualTypeArguments != null && actualTypeArguments.length > 0)) {
                builder.append('<');
                String collect = Arrays.stream(actualTypeArguments).map(typeArg -> {
                    if (typeArg instanceof Class ||
                            typeArg instanceof ParameterizedType ||
                            typeArg instanceof TypeVariable ||
                            typeArg instanceof GenericArrayType ||
                            typeArg instanceof WildcardType) return toString(typeArg);
                    throw new UnsupportedOperationException(typeArg.getClass().getName());
                }).collect(Collectors.joining(", "));
                builder.append(collect).append('>');
            }

            return builder.toString();
        }

        if (type instanceof TypeVariable) {
            TypeVariable tType = (TypeVariable) type;
            if (resolver == null) {
                return tType.getName();
            }

            return resolver.apply(tType);
        }

        if (type instanceof GenericArrayType) {
            return toString(((GenericArrayType) type).getGenericComponentType(), keepSimple, resolver) + "[]";
        }

        if (type instanceof WildcardType) {
            WildcardType wType = (WildcardType) type;
            if (wType.getLowerBounds() != null && wType.getLowerBounds().length > 0) {
                return "? super " + toString(wType.getLowerBounds()[0], keepSimple, resolver);
            } else if (wType.getUpperBounds()[0] == Object.class) {
                return "?";
            } else {
                return "? extends " + toString(wType.getUpperBounds()[0], keepSimple, resolver);
            }
        }

        throw new UnsupportedOperationException(type.getClass().getName());
    }

    static boolean isArray(Type parameterizedType) {
        return parameterizedType instanceof GenericArrayType || (parameterizedType instanceof Class && ((Class) parameterizedType).isArray());
    }
}

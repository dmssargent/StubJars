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
import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import davidsar.gent.stubjars.components.expressions.StringExpression;
import davidsar.gent.stubjars.components.expressions.TypeExpression;
import davidsar.gent.stubjars.components.writer.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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
            Expression typeParams = Utils.arrayToListExpression(typeParameters, typeParam -> {
                if (typeParam.getBounds()[0] == Object.class) {
                    return Expressions.fromString(typeParam.getName());
                } else {
                    return Expressions.fromString(typeParam.getName() + " extends " + JarType.toString(typeParam.getBounds()[0]));
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
    static TypeExpression toExpression(@NotNull Type type) {
        return toExpression(type, false, null);
    }

    @NotNull
    public static String toString(@NotNull Type type, boolean keepSimple, @Nullable Function<TypeVariable, String> resolver) {
        return toExpression(type, keepSimple, resolver).toString();
    }

    @NotNull
    public static TypeExpression toExpression(@NotNull Type type, boolean keepSimple, @Nullable Function<TypeVariable, String> resolver) {
        if (type instanceof Class) {
            return JarClass.safeFullNameForClass((Class<?>) type);
        }

        if (type instanceof ParameterizedType) {
            //StringBuilder builder = new StringBuilder();
            ParameterizedType pType = (ParameterizedType) type;
            TypeExpression ownerTypeExpression;
            if (pType.getOwnerType() != null) {
                Type ownerType = pType.getOwnerType();
                if (ownerType instanceof Class) {
                    ownerTypeExpression = Expressions.forType(pType.getRawType(), JarClass.safeFullNameForClass((Class<?>) pType.getRawType()));
                } else if (ownerType instanceof ParameterizedType) {
                    Class<?> rawTypeOfOwner = (Class<?>) ((ParameterizedType) pType.getOwnerType()).getRawType();
                    ownerTypeExpression = Expressions.forType(rawTypeOfOwner, Expressions.of(
                        toExpression(ownerType),
                        StringExpression.PERIOD,
                        Expressions.forType(pType.getRawType(), Expressions.fromString(
                            ((Class<?>) pType.getRawType()).getName()
                                .replace(rawTypeOfOwner.getName() + "$", Constants.EMPTY_STRING))
                        )
                    ));
                } else {
                    throw new UnsupportedOperationException(type.getClass().getName());
                }
            } else {
                ownerTypeExpression = Expressions.forType(pType.getRawType(),
                    JarClass.safeFullNameForClass((Class<?>) pType.getRawType())
                );
            }

            Type[] actualTypeArguments = pType.getActualTypeArguments();
            Expression typeArgumentExpression = null;
            if (!keepSimple && (actualTypeArguments != null && actualTypeArguments.length > 0)) {
                typeArgumentExpression = Expressions.of(
                    StringExpression.LESS_THAN,
                    Expressions.makeListFrom(Arrays.stream(actualTypeArguments).map(typeArg -> {
                        if (typeArg instanceof Class
                            || typeArg instanceof ParameterizedType
                            || typeArg instanceof TypeVariable
                            || typeArg instanceof GenericArrayType
                            || typeArg instanceof WildcardType) {
                            return toExpression(typeArg);
                        }
                        throw new UnsupportedOperationException(typeArg.getClass().getName());
                    })),
                    StringExpression.GREATER_THAN
                );
            }

            if (typeArgumentExpression == null) {
                return ownerTypeExpression;
            } else {
                return new ParameterizedTypeExpression(type, ownerTypeExpression, typeArgumentExpression);
            }
        }

        if (type instanceof TypeVariable) {
            TypeVariable tType = (TypeVariable) type;
            if (resolver == null) {
                return Expressions.forType(type, Expressions.fromString(tType.getName()));
            }

            return Expressions.forType(tType, Expressions.fromString(resolver.apply(tType)));
        }

        if (type instanceof GenericArrayType) {
            return Expressions.forType(type,
                Expressions.of(
                    toExpression(((GenericArrayType) type).getGenericComponentType(), keepSimple, resolver),
                    Expressions.fromString("[]")
                )
            );
        }

        if (type instanceof WildcardType) {
            WildcardType wType = (WildcardType) type;
            if (wType.getLowerBounds() != null && wType.getLowerBounds().length > 0) {
                return new WildcardSuperType(wType, toExpression(wType.getLowerBounds()[0], keepSimple, resolver));
            } else if (wType.getUpperBounds()[0] == Object.class) {
                return new WildcardTypeExpression();
            } else {
                return new WildcardExtendsType(wType, toExpression(wType.getUpperBounds()[0], keepSimple, resolver));
            }
        }

        throw new UnsupportedOperationException(type.getClass().getName());
    }


    static boolean isArray(Type parameterizedType) {
        return parameterizedType instanceof GenericArrayType
            || (parameterizedType instanceof Class && ((Class) parameterizedType).isArray());
    }

    private static class WildcardBoundedType extends TypeExpression {
        protected List<Expression> children;

        WildcardBoundedType(Type type, Expression typeExpression, TypeExpression boundingType) {
            super(type, null);
            children = Collections.unmodifiableList(Arrays.asList(
                StringExpression.QUESTION_MARK,
                StringExpression.SPACE,
                typeExpression,
                StringExpression.SPACE,
                boundingType
            ));
        }

        @Override
        public boolean hasChildren() {
            return true;
        }

        @Override
        public List<Expression> children() {
            return children;
        }
    }

    private static class WildcardSuperType extends WildcardBoundedType {
        private WildcardSuperType(Type type, TypeExpression boundingType) {
            super(type, StringExpression.SUPER, boundingType);
        }
    }

    private static class WildcardExtendsType extends WildcardBoundedType {
        private WildcardExtendsType(Type type, @NotNull TypeExpression expression) {
            super(type, StringExpression.EXTENDS, expression);
        }
    }

    private static class ParameterizedTypeExpression extends TypeExpression {
        private ParameterizedTypeExpression(@NotNull Type type, TypeExpression ownerTypeExpression, Expression typeArgumentExpression) {
            super(type, Expressions.of(ownerTypeExpression, typeArgumentExpression));
        }
    }

    private static class WildcardTypeExpression extends TypeExpression {
        WildcardTypeExpression() {
            super(Object.class, StringExpression.QUESTION_MARK);
        }
    }

    static class ArrayType extends TypeExpression {
        public ArrayType(@NotNull Class<?> clazz) {
            super(clazz, Expressions.of(
                JarClass.safeFullNameForClass(clazz.getComponentType()),
                StringExpression.LEFT_BRACE,
                StringExpression.RIGHT_BRACE)
            );
        }

    }
}

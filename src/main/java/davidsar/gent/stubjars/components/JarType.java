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

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class JarType {
    private final Type type;

    public JarType(@NotNull Type type) {
        this.type = type;
    }

    public static Expression convertTypeParametersToExpression(TypeVariable<?>[] typeParameters, JarClass<?> against) {
        if (typeParameters.length == 0) {
            return StringExpression.SPACE;
        }

        Expression typeParams = Utils.arrayToListExpression(typeParameters, typeParam -> {
            if (typeParam.getBounds()[0] == Object.class) {
                return Expressions.fromString(typeParam.getName());
            } else {
                return Expressions.of(
                    Expressions.fromString(typeParam.getName()),
                    StringExpression.SPACE,
                    StringExpression.EXTENDS,
                    StringExpression.SPACE,
                    JarType.toExpression(typeParam.getBounds()[0], against));
            }
        });
        return Expressions.of(
            StringExpression.LESS_THAN,
            typeParams,
            StringExpression.GREATER_THAN
        );
    }

    @NotNull
    @Override
    public String toString() {
        return toString(type, null);
    }

    @NotNull
    static String toString(@NotNull Type type, JarClass<?> against) {
        return toExpression(type, against, false, null).toString();
    }

    public static TypeExpression toExpression(@NotNull Type type, JarClass<?> against) {
        return toExpression(type, against, false, null);
    }

    @NotNull
    public static String toString(@NotNull Type type, @NotNull JarClass<?> against, boolean keepSimple, @Nullable Function<TypeVariable, String> resolver) {
        return toExpression(type, against, keepSimple, resolver).toString();
    }

    public static TypeExpression toExpression(@NotNull Type type, JarClass<?> against, boolean keepSimple, @Nullable Function<TypeVariable, String> resolver) {
        if (type instanceof Class) {
            return JarClass.safeFullNameForClass((Class<?>) type, against);
        }

        if (type instanceof ParameterizedType) {
            return parameterizedTypeToExpression((ParameterizedType) type, against, keepSimple);
        }

        if (type instanceof TypeVariable) {
            return typeVariableToExpression(type, resolver);
        }

        if (type instanceof GenericArrayType) {
            return genericArrayTypeToExpression(type, against, keepSimple, resolver);
        }

        if (type instanceof WildcardType) {
            return wildcardTypeToExpression((WildcardType) type, against, keepSimple, resolver);
        }

        throw new UnsupportedOperationException(type.getClass().getName());
    }

    @NotNull
    private static TypeExpression parameterizedTypeToExpression(@NotNull ParameterizedType type, JarClass<?> against, boolean keepSimple) {
        TypeExpression ownerTypeExpression;
        if (type.getOwnerType() != null) {
            ownerTypeExpression = handleOwnerTypeOfParameterizedType(type, against);
        } else {
            ownerTypeExpression = handleRawTypeOfParameterizedType(type, against);
        }

        Expression typeArgumentExpression = handleTypeArgumentsOfParameterizedType(type, against, keepSimple);

        if (typeArgumentExpression == null) {
            return ownerTypeExpression;
        } else {
            return new ParameterizedTypeExpression(type, ownerTypeExpression, typeArgumentExpression);
        }
    }

    private static TypeExpression handleRawTypeOfParameterizedType(@NotNull ParameterizedType type, JarClass<?> against) {
        return Expressions.forType(type.getRawType(),
            JarClass.safeFullNameForClass((Class<?>) type.getRawType(), against)
        );
    }

    @Nullable
    private static Expression handleTypeArgumentsOfParameterizedType(@NotNull ParameterizedType type, JarClass<?> against, boolean keepSimple) {
        Type[] actualTypeArguments = type.getActualTypeArguments();
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
                        return toExpression(typeArg, against);
                    }
                    throw new UnsupportedOperationException(typeArg.getClass().getName());
                })),
                StringExpression.GREATER_THAN
            );
        }
        return typeArgumentExpression;
    }

    @NotNull
    private static TypeExpression handleOwnerTypeOfParameterizedType(@NotNull ParameterizedType type, JarClass<?> against) {
        TypeExpression ownerTypeExpression;
        if (type.getOwnerType() instanceof Class) {
            ownerTypeExpression = handleRawTypeOfParameterizedType(type, against);
        } else if (type.getOwnerType() instanceof ParameterizedType) {
            Class<?> rawTypeOfOwner = (Class<?>) ((ParameterizedType) type.getOwnerType()).getRawType();
            ownerTypeExpression = Expressions.forType(rawTypeOfOwner, Expressions.of(
                toExpression(type.getOwnerType(), against),
                StringExpression.PERIOD,
                Expressions.forType(type.getRawType(), Expressions.fromString(
                    ((Class<?>) type.getRawType()).getName()
                        .replace(rawTypeOfOwner.getName() + "$", Constants.EMPTY_STRING))
                )
            ));
        } else {
            throw new UnsupportedOperationException(type.getClass().getName());
        }

        return ownerTypeExpression;
    }

    @NotNull
    private static TypeExpression typeVariableToExpression(@NotNull Type type, @Nullable Function<TypeVariable, String> resolver) {
        TypeVariable tType = (TypeVariable) type;
        if (resolver == null) {
            return Expressions.forType(type, Expressions.fromString(tType.getName()));
        }

        return Expressions.forType(tType, Expressions.fromString(resolver.apply(tType)));
    }

    @NotNull
    private static TypeExpression genericArrayTypeToExpression(@NotNull Type type, JarClass<?> against, boolean keepSimple, @Nullable Function<TypeVariable, String> resolver) {
        return Expressions.forType(type,
            Expressions.of(
                toExpression(((GenericArrayType) type).getGenericComponentType(), against, keepSimple, resolver),
                Expressions.fromString("[]")
            )
        );
    }

    @NotNull
    private static TypeExpression wildcardTypeToExpression(@NotNull WildcardType type, JarClass<?> against, boolean keepSimple, @Nullable Function<TypeVariable, String> resolver) {
        if (type.getLowerBounds() != null && type.getLowerBounds().length > 0) {
            return new WildcardSuperType(type, toExpression(type.getLowerBounds()[0], against, keepSimple, resolver));
        } else if (type.getUpperBounds()[0] == Object.class) {
            return new WildcardTypeExpression();
        } else {
            return new WildcardExtendsType(type, toExpression(type.getUpperBounds()[0], against, keepSimple, resolver));
        }
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
        public ArrayType(@NotNull Class<?> clazz, JarClass<?> against) {
            super(clazz, Expressions.of(
                JarClass.safeFullNameForClass(clazz.getComponentType(), against),
                StringExpression.LEFT_BRACE,
                StringExpression.RIGHT_BRACE)
            );
        }

    }
}

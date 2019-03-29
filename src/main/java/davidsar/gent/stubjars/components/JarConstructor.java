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
import davidsar.gent.stubjars.components.expressions.CompileableExpression;
import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import davidsar.gent.stubjars.components.expressions.StringExpression;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JarConstructor<T> extends JarModifiers implements CompileableExpression {
    private final JarClass<T> clazz;
    private final Constructor<T> constructor;

    JarConstructor(@NotNull JarClass<T> clazz, @NotNull Constructor<T> constructor) {
        this.clazz = clazz;
        this.constructor = constructor;
    }

    @Override
    protected int getModifiers() {
        return constructor.getModifiers();
    }

    boolean shouldIncludeCotr() {
        for (Class<?> paramType : constructor.getParameterTypes()) {
            if (!JarClass.hasSafeName(paramType)) {
                return false;
            }
        }

        return true;
    }

    @NotNull
    Expression[] parameters() {
        return Arrays.stream(constructor.getParameters())
            .map(parameter -> Expressions.of(
                JarType.toExpression(parameter.getParameterizedType(), clazz),
                StringExpression.SPACE,
                Expressions.fromString(parameter.getName()))
            ).toArray(Expression[]::new);
    }

    @Nullable
    private static Type typeArgumentForClass(@NotNull TypeVariable typeVariable, @NotNull Class<?> clazz) {
        if (clazz.getSuperclass() == null) {
            return null;
        }

        Type superClassType = clazz.getGenericSuperclass();
        if (!(superClassType instanceof ParameterizedType)) {
            return null;
        }

        Class<?> superClass = clazz.getSuperclass();
        ParameterizedType parameterizedSuperType = (ParameterizedType) superClassType;
        Type[] actualTypeParameters = superClass.getTypeParameters();
        Type[] actualTypeArguments = parameterizedSuperType.getActualTypeArguments();
        for (int i = 0; i < actualTypeArguments.length; i++) {
            if ((actualTypeParameters[i] instanceof TypeVariable)) {
                TypeVariable testType = (TypeVariable) actualTypeParameters[i];
                if (!testType.getName().equals(typeVariable.getName())) {
                    continue;
                }

                return actualTypeArguments[i];
            }

            if (handleParameterizedType(typeVariable, actualTypeParameters[i])) {
                return actualTypeArguments[i];
            }
        }

        return null;
    }

    @Contract("_, null -> false")
    private static boolean handleParameterizedType(@NotNull TypeVariable typeVariable, Type actualTypeParameter) {
        if (!(actualTypeParameter instanceof ParameterizedType)) {
            return false;
        }

        ParameterizedType testType = (ParameterizedType) actualTypeParameter;
        for (Type testChildType : testType.getActualTypeArguments()) {
            if (testChildType instanceof TypeVariable) {
                TypeVariable tTestChildType = (TypeVariable) testChildType;
                if (tTestChildType.getName().equals(typeVariable.getName())) {
                    return true;
                }
            }

            if (testChildType instanceof ParameterizedType) {
                return handleParameterizedType(typeVariable, testChildType);
            }
        }

        return false;
    }

    private Expression name() {
        return Expressions.fromString(constructor.getDeclaringClass().getSimpleName());
    }

    private static boolean hasDefaultConstructor(@NotNull Class<?> clazz) {
        try {
            Class<?> declaringClass = clazz.getDeclaringClass();
            Constructor<?> declaredConstructor;
            if (declaringClass == null || Modifier.isStatic(clazz.getModifiers())) {
                declaredConstructor = clazz.getDeclaredConstructor();
            } else {
                return false;
            }

            return Modifier.isPublic(declaredConstructor.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private Constructor<T> getConstructor() {
        return constructor;
    }

    @Override
    public Expression compileToExpression() {
        if (!shouldIncludeCotr()) {
            throw new RuntimeException();
        }

        final Expression security;
        if (clazz.isInterface()) {
            security = StringExpression.EMPTY;
        } else {
            security = security().expression();
        }

        final Expression nameS = name();
        final Expression parametersS;
        if (canRewriteConstructorParams()) {
            parametersS = StringExpression.EMPTY;
        } else {
            parametersS = Utils.arrayToListExpression(parameters());
        }

        final Expression stubMethod = determineBody();
        return new JarConstructorExpression(security, nameS, parametersS, stubMethod);
    }

    @NotNull
    private Expression determineBody() {
        Class<?> clazzSuperClass = clazz.extendsClass();
        Expression stubMethod;// What should the contents of the constructor be?
        if (clazzSuperClass == null || JarConstructor.hasDefaultConstructor(clazzSuperClass)) {
            return Expressions.emptyBlock();
        } else {
            // We need to call some form of the default constructor, so we can compile code
            JarConstructor<?>[] declaredConstructors;
            JarClass<?> jarClass = JarClass.forClass(clazzSuperClass);
            declaredConstructors = jarClass.constructors().toArray(new JarConstructor<?>[0]);
            if (declaredConstructors.length <= 0) {
                throw new UnsupportedOperationException("Cannot infer super cotr to write for " + clazz.fullName());
            }

            JarConstructor selectedCotr = null;
            for (JarConstructor declaredCotr : declaredConstructors) {
                if (declaredCotr.security() == SecurityModifier.PRIVATE) {
                    continue;
                }

                selectedCotr = declaredCotr;
                break;
            }

            if (selectedCotr == null || selectedCotr.canRewriteConstructorParams()) {
                stubMethod = Expressions.toMethodCall("super").asBlock();
            } else {
                Type[] genericParameterTypes = selectedCotr.getConstructor().getGenericParameterTypes();
                stubMethod = Expressions.toMethodCall("super",
                    Utils.arrayToListExpression(genericParameterTypes,
                        paramType -> castedDefaultType(paramType, clazz)
                    )
                ).asBlock();
            }
        }

        return stubMethod;
    }

    boolean canRewriteConstructorParams() {
        return security() == SecurityModifier.PRIVATE;
    }

    @Nullable
    static Expression castedDefaultType(Type paramType, JarClass<?> clazz) {
        final Type correctType;
        if (paramType instanceof TypeVariable) {
            Type testCorrectType = typeArgumentForClass((TypeVariable) paramType, clazz.getClazz());
            if (testCorrectType == null) {
                correctType = paramType;
            } else {
                correctType = testCorrectType;
            }
        } else {
            correctType = paramType;
        }

        if (correctType.equals(Void.class)) {
            return null;
        }

        return Expressions.toCast(JarType.toExpression(correctType, clazz, true, type -> {
            Type obj = typeArgumentForClass(type, clazz.getClazz());
            return JarType.toString(obj != null ? obj : type, clazz);
        }), Value.defaultValueForType(correctType, clazz));
    }

    private static class JarConstructorExpression extends Expression {
        private List<Expression> children;

        private JarConstructorExpression(Expression security, Expression nameS, Expression parametersS, Expression stubMethod) {
            children = Collections.unmodifiableList(Arrays.asList(
                security,
                StringExpression.SPACE,
                nameS,
                parametersS.parenthetical(),
                StringExpression.SPACE,
                stubMethod
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
}

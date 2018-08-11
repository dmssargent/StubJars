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

import static davidsar.gent.stubjars.components.writer.Constants.EMPTY_STRING;

import davidsar.gent.stubjars.Utils;
import davidsar.gent.stubjars.components.expressions.CompileableExpression;
import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import davidsar.gent.stubjars.components.expressions.StringExpression;
import davidsar.gent.stubjars.components.writer.Constants;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

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
    String[] parameters() {
        return Arrays.stream(constructor.getParameters())
                .map(parameter -> JarType.toString(parameter.getParameterizedType()) + Constants.SPACE + parameter.getName())
                .toArray(String[]::new);
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

    @NotNull
    private String name() {
        return constructor.getDeclaringClass().getSimpleName();
    }

    private static boolean hasDefaultConstructor(@NotNull Class<?> klazz) {
        try {
            Class<?> declaringClass = klazz.getDeclaringClass();
            Constructor<?> declaredConstructor;
            if (declaringClass == null || Modifier.isStatic(klazz.getModifiers())) {
                declaredConstructor = klazz.getDeclaredConstructor();
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

        final String security;
        if (clazz.isInterface()) {
            security = EMPTY_STRING;
        } else {
            security = security().getModifier() + (security() == SecurityModifier.PACKAGE ? EMPTY_STRING : Constants.SPACE);
        }
        final String nameS = name();
        final Expression parametersS;
        if (canRewriteConstructorParams()) {
            parametersS = StringExpression.EMPTY;
        } else {
            parametersS = Utils.arrayToListExpression(parameters(), Expressions::fromString);
        }
        Class<?> clazzSuperClass = clazz.extendsClass();

        final Expression stubMethod;
        // What should the contents of the constructor be?
        if (clazzSuperClass == null || JarConstructor.hasDefaultConstructor(clazzSuperClass)) {
            stubMethod = StringExpression.EMPTY;
        } else {
            // We need to call some form of the default constructor, so we can compile code
            JarConstructor<?>[] declaredConstructors;
            JarClass<?> jarClass = JarClass.forClass(clazzSuperClass);
            declaredConstructors = jarClass.constructors().toArray(new JarConstructor<?>[0]);
            if (declaredConstructors.length <= 0) {
                throw new UnsupportedOperationException("Cannot infer super cotr to write for " + clazz.getClazz().getName());
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
                stubMethod = Expressions.toMethodCall("super").indent();
            } else {
                Type[] genericParameterTypes = selectedCotr.getConstructor().getGenericParameterTypes();
                stubMethod = Expressions.toMethodCall("super",
                    Utils.arrayToListExpression(genericParameterTypes,
                        paramType -> castedDefaultType(paramType, clazz)
                    )
                ).indent();
            }
        }

        return Expressions.fromString(
            String.format("%s%s%s%s %s\n\n",
                Constants.NEW_LINE_CHARACTER, security, nameS,
                Expressions.asParenthetical(parametersS), Expressions.blockWith(stubMethod))
        );
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

        return Expressions.toCast(JarType.toExpression(correctType, true, type -> {
            Type obj = typeArgumentForClass(type, clazz.getClazz());
            return JarType.toString(obj != null ? obj : type);
        }), Value.defaultValueForType(correctType));
    }
}

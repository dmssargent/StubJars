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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;

import static davidsar.gent.stubjars.components.writer.Constants.EMPTY_STRING;
import static davidsar.gent.stubjars.components.writer.Constants.INDENT;

public class JarConstructor<T> extends JarModifiers implements CompileableExpression {
    private final JarClass<T> klazz;
    private final Constructor<T> constructor;

    JarConstructor(@NotNull JarClass<T> klazz, @NotNull Constructor<T> constructor) {
        this.klazz = klazz;
        this.constructor = constructor;
    }

    @Override
    protected int getModifiers() {
        return constructor.getModifiers();
    }

    boolean shouldIncludeCotr() {
        for (Class<?> paramType : constructor.getParameterTypes()) {
            if (!JarClass.hasSafeName(paramType)) return false;
        }

        return true;
    }

    @NotNull
    public String[] parameters() {
        return Arrays.stream(constructor.getParameters())
                .map(parameter -> JarType.toString(parameter.getParameterizedType()) + Constants.SPACE + parameter.getName())
                .toArray(String[]::new);
    }

    @Nullable
    private static Type typeArgumentForClass(@NotNull TypeVariable typeVariable, @NotNull Class<?> klazz) {
        if (klazz.getSuperclass() == null) return null;
        Type superClassType = klazz.getGenericSuperclass();
        if (!(superClassType instanceof ParameterizedType)) return null;
        Class<?> superClass = klazz.getSuperclass();
        ParameterizedType pSuperClassType = (ParameterizedType) superClassType;
        Type[] actualTypeParameters = superClass.getTypeParameters();
        Type[] actualTypeArguments = pSuperClassType.getActualTypeArguments();
        for (int i = 0; i < actualTypeArguments.length; i++) {
            if ((actualTypeParameters[i] instanceof TypeVariable)) {
                TypeVariable testType = (TypeVariable) actualTypeParameters[i];
                if (!testType.getName().equals(typeVariable.getName())) continue;

                return actualTypeArguments[i];
            }

            if (handleParameterizedType(typeVariable, actualTypeParameters[i]))
                return actualTypeArguments[i];
        }

        return null;
    }

    @Contract("_, null -> false")
    private static boolean handleParameterizedType(@NotNull TypeVariable typeVariable, Type actualTypeParameter) {
        if (!(actualTypeParameter instanceof ParameterizedType)) return false;

        ParameterizedType testType = (ParameterizedType) actualTypeParameter;
        for (Type testChildType : testType.getActualTypeArguments()) {
            if (testChildType instanceof TypeVariable) {
                TypeVariable tTestChildType = (TypeVariable) testChildType;
                if (tTestChildType.getName().equals(typeVariable.getName()))
                    return true;
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
            if (declaringClass == null || Modifier.isStatic(klazz.getModifiers()))
                declaredConstructor = klazz.getDeclaredConstructor();
            else
                return false;

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
        if (!shouldIncludeCotr()) throw new RuntimeException();
        final String security;
        if (klazz.isInterface())
            security = EMPTY_STRING;
        else
            security = security().getModifier() + (security() == SecurityModifier.PACKAGE ? EMPTY_STRING : Constants.SPACE);
        final String nameS = name();
        final String parametersS;
        if (canRewriteConstructorParams()) {
            parametersS = Constants.EMPTY_STRING;
        } else {
            parametersS = Utils.arrayToCommaSeparatedList(parameters(), x -> x);
        }
        Class<?> klazzSuperClass = klazz.extendsClass();

        final String stubMethod;
        // What should the contents of the constructor be?
        if (klazzSuperClass == null || JarConstructor.hasDefaultConstructor(klazzSuperClass)) {
            stubMethod = EMPTY_STRING;
        } else {
            // We need to call some form of the default constructor, so we can compile code
            JarConstructor<?>[] declaredConstructors;
            JarClass<?> jarClass = JarClass.forClass(klazzSuperClass);
            declaredConstructors = jarClass.constructors().toArray(new JarConstructor<?>[0]);
            if (declaredConstructors.length <= 0)
                throw new UnsupportedOperationException("Cannot infer super cotr to write for " + klazz.getKlazz().getName());
            JarConstructor selectedCotr = null;
            for (JarConstructor declaredCotr : declaredConstructors) {
                if (declaredCotr.security() == SecurityModifier.PRIVATE) {
                    continue;
                }

                selectedCotr = declaredCotr;
                break;
            }

            if (selectedCotr == null || selectedCotr.canRewriteConstructorParams()) {
                stubMethod = Expression.methodCall("super").toString();
            } else {
                Type[] genericParameterTypes = selectedCotr.getConstructor().getGenericParameterTypes();
                stubMethod = String.format("%s%s", INDENT, Expression.methodCall("super",
                        Utils.arrayToCommaSeparatedList(genericParameterTypes,
                                paramType -> castedDefaultType(paramType, klazz)
                        ))
                );
            }
        }

        return Expression.of(String.format("%s%s%s%s %s\n\n", Constants.NEW_LINE_CHARACTER, security, nameS, Expression.parenthetical(parametersS), Expression.block(stubMethod)));
    }

    boolean canRewriteConstructorParams() {
        return security() == SecurityModifier.PRIVATE;
    }

    @Nullable
    static String castedDefaultType(Type paramType, JarClass<?> klazz) {
        final Type correctType;
        if (paramType instanceof TypeVariable) {
            Type testCorrectType = typeArgumentForClass((TypeVariable) paramType, klazz.getKlazz());
            if (testCorrectType == null) {
                correctType = paramType;
            } else {
                correctType = testCorrectType;
            }
        } else {
            correctType = paramType;
        }

        if (correctType.equals(Void.class)) return null;
        return Expression.cast(JarType.toString(correctType, true, type -> {
            Type obj = typeArgumentForClass(type, klazz.getKlazz());
            return JarType.toString(obj != null ? obj : type);
        }), Value.defaultValueForType(correctType));
    }
}

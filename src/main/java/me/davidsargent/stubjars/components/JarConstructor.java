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

package me.davidsargent.stubjars.components;

import me.davidsargent.stubjars.Utils;
import me.davidsargent.stubjars.components.writer.JavaClassWriter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;

import static me.davidsargent.stubjars.components.writer.Constants.INDENT;

public class JarConstructor<T> extends JarModifers implements CompileableString {
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
        Parameter[] parameters = constructor.getParameters();
        return Arrays.stream(parameters)
                .map(parameter -> JarType.toString(parameter.getParameterizedType()) + " " + parameter.getName())
                .toArray(String[]::new);
    }

    @Nullable
    public static Type typeArgumentForClass(@NotNull TypeVariable typeVariable, @NotNull Class<?> klazz) {
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
    public String name() {
        return constructor.getDeclaringClass().getSimpleName();
    }

    public static boolean hasDefaultConstructor(@NotNull JarClass<?> klazz) {
        return hasDefaultConstructor(klazz.getKlazz());
    }

    public boolean isDefaultConstructor() {
        return isDefaultConstructor(constructor);
    }

    private static boolean isDefaultConstructor(@NotNull Constructor<?> constructor) {
        Class<?> klazz = constructor.getDeclaringClass();
        if (klazz.getDeclaringClass() == null || Modifier.isStatic(klazz.getModifiers()))
            return constructor.getParameterCount() == 0;
        else
            return constructor.getParameterCount() == 1 && constructor.getGenericParameterTypes()[0].equals(klazz.getDeclaringClass());
    }

    public static boolean hasDefaultConstructor(@NotNull Class<?> klazz) {
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

    public Constructor<T> getConstructor() {
        return constructor;
    }

    @Override
    public String compileToString() {
        final String EMPTY_STRING = "";
        final String security;
        if (klazz.isInterface())
            security = EMPTY_STRING;
        else
            security = security().getModifier() + (security() == SecurityModifier.PACKAGE ? EMPTY_STRING : " ");
        final String nameS = name();
        final String parametersS = Utils.arrayToCommaSeparatedList(parameters(), x -> x);
        Class<?> klazzSuperClass = klazz.extendsClass();

        final String stubMethod;
        // What should the contents of the constructor be?
        if (klazzSuperClass == null || JarConstructor.hasDefaultConstructor(klazzSuperClass)) {
            stubMethod = EMPTY_STRING;
        } else {
            // We need to call some form of the default constructor, so we can compile code
            JarConstructor<?>[] declaredConstructors;
            declaredConstructors = (JarConstructor<?>[]) JarClass.forClass(klazzSuperClass).constructors().toArray(new JarConstructor<?>[0]);
            if (declaredConstructors.length <= 0)
                throw new UnsupportedOperationException("Cannot infer super cotr to write for " + klazz.getKlazz().getName());
            JarConstructor selectedCotr = null;
            for (JarConstructor declaredCotr : declaredConstructors) {
                if (declaredCotr.security() == SecurityModifier.PRIVATE) continue;
                selectedCotr = declaredCotr;
                break;
            }

            if (selectedCotr == null) {
                stubMethod = "super();";
            } else {
                Type[] genericParameterTypes = selectedCotr.getConstructor().getGenericParameterTypes();
                stubMethod = String.format("%ssuper(%s);", INDENT, Utils.arrayToCommaSeparatedList(
                        genericParameterTypes, paramType -> {
//                            if (!klazz.isStatic() && klazz.isInnerClass() && paramType.equals(klazz.getKlazz().getDeclaringClass())) {
//                                return null;
//                            }
                            return castedDefaultType(paramType, klazz);
                        }
                ));
            }
        }

        return String.format("%s%s%s(%s) {\n%s\n}\n\n", '\n', security, nameS, parametersS, stubMethod);
    }

    @Nullable
    public static String castedDefaultType(Type paramType, JarClass<?> klazz) {
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
        return String.format("(%s) %s", JarType.toString(correctType, true, type -> {
            Type obj = typeArgumentForClass(type, klazz.getKlazz());
            return JarType.toString(obj != null ? obj : type);
        }), JavaClassWriter.defaultValueForType(correctType));
    }
}

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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;

public class JarConstructor<T> extends JarModifers {
    private final Constructor<T> constructor;

    JarConstructor(@NotNull Constructor<T> constructor) {
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
        Class<?> superClass = klazz.getSuperclass();
        if (!(superClassType instanceof ParameterizedType))  return null;
        ParameterizedType pSuperClassType = (ParameterizedType) superClassType;
        Type[] actualTypeArguments = pSuperClassType.getActualTypeArguments();
        Type[] actualTypeParameters = superClass.getTypeParameters();
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

    private static boolean handleParameterizedType(@NotNull TypeVariable typeVariable, Type actualTypeParameter) {
        if ((actualTypeParameter) instanceof ParameterizedType) {
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
//            else {
//                declaredConstructor = klazz.getDeclaredConstructor(declaringClass);
//            }
            else
                return false;

            return !Modifier.isPrivate(declaredConstructor.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public Constructor<T> getConstructor() {
        return constructor;
    }
}

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

import me.davidsargent.stubjars.JarFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JarClass<T> extends JarModifers {
    private final static Pattern classEntryPatternToBeStripped = Pattern.compile("\\.class$");
    private static Map<Class<?>, JarClass<?>> classToJarClassMap;
    private final ClassLoader classLoader;
    private final Class<T> klazz;
    private final JarFile jarFile;
    private final String entryName;

    public JarClass(@NotNull ClassLoader classLoader, JarFile jarFile, @NotNull String entryName) throws ClassNotFoundException {
        this.classLoader = classLoader;
        this.jarFile = jarFile;
        this.entryName = entryName;

        String convertedName = convertEntryNameToClassName(entryName);
        klazz = (Class<T>) Class.forName(convertedName, false, classLoader);
    }

    private JarClass(ClassLoader classLoader, JarFile jarFile, String entryName, @NotNull Class<T> klazz) throws ClassNotFoundException {
        this.classLoader = classLoader;
        this.jarFile = jarFile;
        this.entryName = entryName;

        this.klazz = klazz;
    }

    public static void loadClassToJarClassMap(@NotNull Map<Class<?>, @NotNull JarClass<?>> map) {
        classToJarClassMap = map;
    }

    @NotNull
    public static <T> JarClass forClass(@NotNull Class<T> klazz) {
        if (classToJarClassMap != null && classToJarClassMap.containsKey(klazz)) return classToJarClassMap.get(klazz);

        try {
            return new JarClass<>(klazz.getClassLoader(), null, null, klazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static String convertEntryNameToClassName(@NotNull String entryName) {
        Matcher matcher = classEntryPatternToBeStripped.matcher(entryName);
        if (matcher.find()) entryName = matcher.replaceAll("");
        return entryName.replace('/', '.');
    }

    public boolean isEnum() {
        Class<?> extendClazz = extendsClass();
        return klazz.isEnum();
    }

    @NotNull
    public Class<?> getKlazz() {
        return klazz;
    }

    public boolean isInnerClass() {
        return klazz.getDeclaringClass() != null || klazz.isLocalClass() || klazz.isAnonymousClass() ;
    }

    @NotNull
    public String name() {
        return klazz.getSimpleName();
    }

    @NotNull
    public String fullName() {
        return safeFullNameForClass(klazz);
    }

    public static boolean hasSafeName(@NotNull Class<?> klazz) {
        JarClass jarClass = JarClass.forClass(klazz);
        return !jarClass.klazz.isSynthetic() && !jarClass.klazz.isAnonymousClass();
    }

    @NotNull
    public static String safeFullNameForClass(@NotNull Class<?> klazz) {
        if (!hasSafeName(klazz))
            throw new IllegalArgumentException("Class does not have safe name.");
        if (klazz.isArray())
            return safeFullNameForClass(klazz.getComponentType()) + "[]";
        String s = klazz.getName().replaceAll("\\$\\d*", ".");
        if (s.endsWith("."))
            throw new IllegalArgumentException("Class does not have safe name.");
        return s;
    }

    @NotNull
    public String packageName() {
        return klazz.getPackage().getName();
    }

    @Nullable
    public Class<?> extendsClass() {
        Class<?> superclass = klazz.getSuperclass();
        return superclass == Object.class ? null : superclass;
    }

    @NotNull
    public Class<?>[] implementsInterfaces() {
        return klazz.getInterfaces();
    }

    @NotNull
    public Type[] implementsGenericInterfaces() {
        return klazz.getGenericInterfaces();
    }

    public boolean isInterface() {
        return klazz.isInterface();
    }

    public boolean isAnnotation() {
        return klazz.isAnnotation();
    }

    @Override
    protected int getModifiers() {
        return klazz.getModifiers();
    }

    @NotNull
    public JarClass[] innerClasses() {
        Stream<Class<?>> innerClassesStream = Arrays.stream(klazz.getDeclaredClasses())
                .filter(klazz -> !klazz.isLocalClass())
                .filter(klazz -> !klazz.isAnonymousClass());

        List<JarClass> collect = innerClassesStream.map(JarClass::forClass)
                .collect(Collectors.toList());

        return collect.toArray(new JarClass[] {});
    }

    @NotNull
    public Set<JarMethod> methods() {
        return Arrays.stream(klazz.getDeclaredMethods())
                .map(JarMethod::new)
                .filter(method -> method.security() != SecurityModifier.PRIVATE)
                .filter(method -> !method.isSynthetic())
                .filter(JarMethod::shouldIncludeStaticMethod)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JarClass && klazz.equals(((JarClass) o).klazz);
    }

    @Override
    public int hashCode() {
        return klazz.hashCode();
    }

    @NotNull
    public Set<JarClass> allSuperClassesAndInterfaces() {
        return allSuperClassesAndInterfaces(klazz).stream().map(JarClass::forClass).collect(Collectors.toSet());
    }

    @NotNull
    private static Set<Class<?>> allSuperClassesAndInterfaces(@NotNull Class<?> klazz) {
        Set<Class<?>> superClasses = allSuperClasses(klazz, new HashSet<>());
        Set<Class<?>> interfaces = new HashSet<>();
        Collections.addAll(interfaces, klazz.getInterfaces());
        for (Class<?> kInterface : superClasses) {
            Collections.addAll(interfaces, kInterface.getInterfaces());
        }

        superClasses.addAll(interfaces);
        return superClasses;
    }

    @NotNull
    private static Set<Class<?>> allSuperClasses(@NotNull Class<?> klazz, @NotNull Set<Class<?>> superClasses) {
        Class<?> superClass = klazz.getSuperclass();
        if (superClass == null )
            return superClasses;
        superClasses.add(superClass);
        return allSuperClasses(superClass, superClasses);
    }

    boolean hasMethod(@NotNull Method method) {
        try {
            Method declaredMethodSuper = klazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @NotNull
    public Type extendsGenericClass() {
        return klazz.getGenericSuperclass();
    }

    @NotNull
    public Set<JarConstructor<?>> constructors() {
        return Arrays.stream(klazz.getDeclaredConstructors())
                .map((Function<Constructor<?>, ? extends JarConstructor<?>>) JarConstructor::new)
               // .filter(cotr -> cotr.security() != SecurityModifier.PRIVATE)
                .filter(JarConstructor::shouldIncludeCotr)
                .collect(Collectors.toSet());
    }
}

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
import davidsar.gent.stubjars.components.expressions.AnnotationExpression;
import davidsar.gent.stubjars.components.expressions.ClassHeaderExpression;
import davidsar.gent.stubjars.components.expressions.CompileableExpression;
import davidsar.gent.stubjars.components.expressions.EnumMembers;
import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import davidsar.gent.stubjars.components.expressions.StringExpression;
import davidsar.gent.stubjars.components.expressions.TypeExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static davidsar.gent.stubjars.components.writer.Constants.EMPTY_STRING;

public class JarClass<T> extends JarModifiers implements CompileableExpression {
    private static final Logger log = LoggerFactory.getLogger(JarClass.class);
    private static final Pattern classEntryPatternToBeStripped = Pattern.compile("\\.class$");
    private static Map<String, JarClass<?>> classToJarClassMap;

    private Class<T> clazz;
    private ClassLoader stubClassLoader;
    private Map<String, JarConstructor> constructors;
    private Map<String, JarMethod> methods;
    private Map<String, JarClass<?>> innerClasses;

    /**
     * Represents a {@link Class} for in StubJars.
     *
     * @param classLoader a {@link ClassLoader} that has access to the given entry
     * @param entryName   a file path representative of request {@code Class} with the full path
     * @throws ClassNotFoundException if the given {@code classLoader} doesn't have access to
     *                                the {@code Class} derived from the {@code entryName}
     */
    public JarClass(@NotNull ClassLoader classLoader, @NotNull String entryName) throws ClassNotFoundException {
        String convertedName = convertEntryNameToClassName(entryName);
        //noinspection unchecked
        clazz = (Class<T>) Class.forName(convertedName, false, classLoader);
        log.debug("loading class: {}", clazz.getName());
        stubClassLoader = classLoader;
    }

    private JarClass(@NotNull Class<T> clazz) {
        this.clazz = clazz;
    }

    public static void loadJarClassList(@NotNull List<JarClass<?>> list) {
        TreeMap<String, JarClass<?>> map = new TreeMap<>();
        for (JarClass<?> a : list) {
            if (map.put(a.clazz.getName(), a) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }

        classToJarClassMap = Collections.synchronizedSortedMap(map);
    }

    @NotNull
    static <T> JarClass<?> forClass(@NotNull Class<T> clazz) {
        if (classToJarClassMap != null && classToJarClassMap.containsKey(clazz.getName())) {
            return classToJarClassMap.get(clazz.getName());
        }

        return new JarClass<>(clazz);
    }

    @NotNull
    private static String convertEntryNameToClassName(@NotNull String entryName) {
        Matcher matcher = classEntryPatternToBeStripped.matcher(entryName);
        if (matcher.find()) {
            entryName = matcher.replaceAll(EMPTY_STRING);
        }
        return entryName.replace('/', '.');
    }

    public boolean isEnum() {
        return clazz.isEnum();
    }

    @NotNull
    public Class<T> getClazz() {
        return clazz;
    }

    public boolean isInnerClass() {
        return clazz.getDeclaringClass() != null || clazz.isLocalClass() || clazz.isAnonymousClass();
    }

    @NotNull
    public String fullName() {
        return clazz.getName();
    }

    @NotNull
    public String name() {
        return clazz.getSimpleName();
    }

    static boolean hasSafeName(@NotNull Class<?> clazz) {
        return !clazz.isSynthetic() && !clazz.isAnonymousClass();
    }

    static TypeExpression safeFullNameForClass(@NotNull Class<?> clazz, JarClass<?> against) {
        if (!hasSafeName(clazz)) {
            throw new IllegalArgumentException("Class does not have safe name.");
        }

        if (clazz.isArray()) {
            return new JarType.ArrayType(clazz, against);
        }

        String rawName;
        final Package againstPackage = against != null ? against.getClazz().getPackage() : null;
        if (againstPackage != null && clazz.getPackage() != null && clazz.getDeclaringClass() == null
            && clazz.getPackage().getName().equals(againstPackage.getName())) {
            rawName = clazz.getSimpleName();
        } else if (clazz.getPackage() != null && clazz.getPackage().getName().equals("java.lang")) {
            rawName = clazz.getSimpleName();
        } else {
            rawName = clazz.getName();
        }

        String s = rawName.replaceAll("\\$\\d*", ".");
        if (s.endsWith(".")) {
            throw new IllegalArgumentException("Class does not have safe name.");
        }

        return Expressions.forType(clazz, Expressions.fromString(s));
    }

    @NotNull
    public String packageName() {
        return clazz.getPackage().getName();
    }

    @Nullable
    Class<?> extendsClass() {
        Class<?> superclass = clazz.getSuperclass();
        return superclass == Object.class ? null : superclass;
    }

    @NotNull
    private Class<?>[] implementsInterfaces() {
        return clazz.getInterfaces();
    }

    @NotNull
    private Type[] implementsGenericInterfaces() {
        return clazz.getGenericInterfaces();
    }

    public boolean isInterface() {
        return clazz.isInterface();
    }

    public boolean isAnnotation() {
        return clazz.isAnnotation();
    }

    @Override
    protected int getModifiers() {
        return clazz.getModifiers();
    }

    private Map<String, JarField> fields() {
        return Arrays.stream(clazz.getDeclaredFields())
            .map(field -> new JarField(this, field))
            .filter(field -> field.security() != SecurityModifier.PRIVATE)
            .collect(Collectors.toMap(x -> x.compileToExpression().toString(), Function.identity(), (x, y) -> y, TreeMap::new));
    }

    /**
     * Returns the {@link Set} of inner classes that StubJars considers a possible public facing API
     * or are required for the resulting Java code to be valid.
     *
     * @return the {@code Set} of inner classes
     */
    public Map<String, JarClass<?>> innerClasses() {
        if (innerClasses == null) {
            innerClasses = Arrays.stream(clazz.getDeclaredClasses())
                .filter(clazz -> !clazz.isLocalClass())
                .filter(clazz -> !clazz.isAnonymousClass())
                .map(JarClass::forClass).collect(Collectors.toMap(x -> x.clazz.getName(), Function.identity(), (x, y) -> y, TreeMap::new));
        }

        return innerClasses;
    }

    @NotNull
    private Map<String, JarMethod> methods() {
        if (methods == null) {
            methods = Arrays.stream(clazz.getDeclaredMethods())
                .map(method -> new JarMethod(this, method))
                .filter(method -> method.security() != SecurityModifier.PRIVATE)
                .filter(method -> !method.isSynthetic())
                .filter(JarMethod::shouldIncludeStaticMethod)
                .collect(Collectors.toMap(x -> x.compileToExpression().toString(), x -> x, (x, y) -> y,  TreeMap::new));
        }
        return methods;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JarClass && clazz.equals(((JarClass) o).clazz);
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

    @NotNull Map<String, JarClass> allSuperClassesAndInterfaces() {
        return allSuperClassesAndInterfaces(clazz).stream().map(JarClass::forClass).collect(Collectors.toMap(x -> x.clazz.getName(), Function.identity(), (x, y) -> y, TreeMap::new));
    }

    @NotNull
    private static Set<Class<?>> allSuperClassesAndInterfaces(@NotNull Class<?> clazz) {
        Set<Class<?>> superClasses = allSuperClasses(clazz, new HashSet<>());
        Set<Class<?>> interfaces = new HashSet<>();
        Collections.addAll(interfaces, clazz.getInterfaces());
        for (Class<?> superClazz : superClasses) {
            Collections.addAll(interfaces, superClazz.getInterfaces());
        }

        superClasses.addAll(interfaces);
        return superClasses;
    }

    @NotNull
    private static Set<Class<?>> allSuperClasses(@NotNull Class<?> klazz, @NotNull Set<Class<?>> superClasses) {
        Class<?> superClass = klazz.getSuperclass();
        if (superClass == null) {
            return superClasses;
        }
        superClasses.add(superClass);
        return allSuperClasses(superClass, superClasses);
    }

    boolean hasMethod(@NotNull Method method) {
        try {
            var ignored = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @NotNull
    private Type extendsGenericClass() {
        return clazz.getGenericSuperclass();
    }

    @NotNull Map<String, JarConstructor> constructors() {
        //noinspection unchecked
        if (constructors == null) {
            //noinspection unchecked
            constructors = Arrays.stream(clazz.getDeclaredConstructors())
                    .map(x -> new JarConstructor(this, x))
                    .filter(JarConstructor::shouldIncludeCotr)
                    .collect(Collectors.toMap(x -> x.compileToExpression().toString(), Function.identity(), (x, y) -> y, TreeMap::new));

            constructors.values().stream()
                    .filter(JarConstructor::canRewriteConstructorParams).findAny()
                    .ifPresent(jarConstructor -> constructors = Stream.concat(
                        constructors.values().stream()
                            .filter(constructor -> !(constructor.canRewriteConstructorParams()
                                || constructor.parameters().length == 0)
                            ),
                            Stream.of(jarConstructor)
                    ).collect(Collectors.toMap(x -> x.compileToExpression().toString(), Function.identity(), (x, y) -> y, TreeMap::new)));
        }

        return constructors;
    }

    @Override
    public Expression compileToExpression() {
        try {
            return compileClass(false, null);
        } catch (NoClassDefFoundError ex) {
            try {
                //noinspection unchecked
                clazz = (Class<T>) Class.forName(fullName(), false, new URLClassLoader(((URLClassLoader) stubClassLoader).getURLs(), stubClassLoader.getParent()));
                return compileClass(false, null);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                log.warn("Missing class definition for {}", fullName(), e);
                return StringExpression.EMPTY;
            }
        }
    }

    @NotNull
    private Expression compileClass(boolean isEnumConstant, String enumName) {
        final Expression methods = compileMethods(isEnumConstant);
        final Expression fields = compileFields(isEnumConstant);
        final Expression constructors = compileConstructors(isEnumConstant);
        final Expression innerClasses = compileInnerClasses(isEnumConstant);
        final Expression clazzHeader = compileHeader(isEnumConstant, enumName);

        // Enums need to be handled quite a bit differently, but we also need to check if we are working on
        // an enum constant to prevent infinite recursion
        if (isEnum() && !isEnumConstant) {
            return handleEnumClass(methods, fields, clazzHeader, innerClasses);
        }

        return new ClassExpression(methods, fields, constructors, innerClasses, clazzHeader);
    }

    @SuppressWarnings("GetClassOnEnum")
    @NotNull
    private Expression handleEnumClass(Expression methods, Expression fields, Expression clazzHeader, Expression innerClasses) {
        Expression enumMembers = StringExpression.EMPTY.asStatement();
        Enum<?>[] invokedExpression = getEnumConstants();

        if (invokedExpression != null) {
            enumMembers = new EnumMembers(Arrays.stream(invokedExpression)
                    .map(member -> JarClass.forClass(member.getClass()).compileClass(true, member.name()))
                .toArray(Expression[]::new)).asStatement();
        }

        return new ClassExpression(methods, enumMembers, fields, innerClasses, Expressions.of(clazzHeader));
    }

    private Expression compileFields(boolean isEnumConstant) {
        return Expressions.indent(fields().values().stream()
            .filter(field -> !((isEnumConstant || isEnum()) && field.isStatic()) && !field.isSynthetic())
                .filter(field -> {
                    Class<?> superClazz = field.getClazz().extendsClass();
                    if (superClazz != null) {
                        try {
                            var ignored = superClazz.getDeclaredField(field.name());
                            return false;
                        } catch (NoSuchFieldException ignored) {
                            // If the field does not exist in the super class, we can safely include it
                        }
                    }

                    return true;
                })
            .map(JarField::compileToExpression)
            .toArray(Expression[]::new));
    }

    private Expression compileMethods(boolean isEnumConstant) {
        return Expressions.indent(methods().values().stream()
            .map(method -> method.compileToExpression(isEnumConstant))
            .toArray(Expression[]::new));
    }

    @NotNull
    private Expression compileInnerClasses(boolean isEnumConstant) {
        var innerClasses = innerClasses();
        if (innerClasses.size() == 0 || isEnumConstant) {
            return StringExpression.EMPTY;
        }

        return Expressions.indent(innerClasses.values().stream()
            .map(JarClass::compileToExpression)
            .toArray(Expression[]::new));
    }

    @NotNull
    private Expression compileConstructors(boolean isEnumConstant) {
        // Interfaces and enum constants don't have constructors
        if (isEnumConstant || isInterface()) {
            return StringExpression.EMPTY;
        }

        return Expressions.indent(constructors().values().stream()
            .map(JarConstructor::compileToExpression)
            .toArray(Expression[]::new));
    }

    @NotNull
    private Expression compileHeader(boolean isEnumConstant, String enumName) {
        if (isEnumConstant) {
            return Expressions.fromString(enumName);
        }

        return new ClassHeaderExpression(this);
    }

    @NotNull
    public Expression compileHeaderImplements() {
        Expression implementsS = StringExpression.EMPTY;
        if (implementsInterfaces().length > 0
            && !(isAnnotation()
            && implementsInterfaces().length == 1)) {
            implementsS = clazz.isInterface()
                ? Expressions.fromString("extends") : Expressions.fromString("implements");
            implementsS = Expressions.of(implementsS, StringExpression.SPACE,
                Utils.arrayToListExpression(implementsGenericInterfaces(), x -> {
                    if (x.equals(Annotation.class)) {
                        return null;
                    }

                    return JarType.toExpression(x, this);
                })
            );
        }

        return implementsS;
    }

    @NotNull
    public Expression compileHeaderExtends() {
        Class<?> extendsClazz = extendsClass();
        if (extendsClazz != null && !extendsClazz.equals(Enum.class)) {
            return Expressions.of(
                StringExpression.EXTENDS,
                StringExpression.SPACE,
                JarType.toExpression(extendsGenericClass(), this)
            );
        } else {
            return StringExpression.EMPTY;
        }
    }

    public Expression compileTypeParameters() {
        return JarType.convertTypeParametersToExpression(getClazz().getTypeParameters(), this);
    }

    @NotNull
    public Expression compileHeaderAnnotation() {
        Expression annotationS;
        if (isAnnotation() && getClazz().isAnnotationPresent(Retention.class)) {
            RetentionPolicy retentionPolicy = getClazz().getAnnotation(Retention.class).value();
            annotationS = new AnnotationExpression(this, Retention.class, retentionPolicy.name());
            if (getClazz().isAnnotationPresent(Deprecated.class)) {
                annotationS = Expressions.of(StringExpression.ANNOTATION_DEPRECATED, StringExpression.SPACE, annotationS);
            }
        } else {
            if (getClazz().isAnnotationPresent(Deprecated.class)) {
                annotationS = StringExpression.ANNOTATION_DEPRECATED;
            } else {
                annotationS = StringExpression.EMPTY;
            }
        }
        return annotationS;
    }

    @NotNull
    public static Expression typeString(@NotNull JarClass<?> clazz) {
        if (clazz.isAnnotation()) {
            return StringExpression.ANNOTATION_TYPE;
        } else if (clazz.isInterface()) {
            return StringExpression.INTERFACE;
        } else if (clazz.isEnum()) {
            return StringExpression.ENUM;
        }

        return StringExpression.CLASS;
    }

    private <E extends Enum<?>> E[] getEnumConstants() {
        if (!isEnum()) {
            throw new IllegalArgumentException("Not an enum");
        }

        //noinspection unchecked
        return JarClass.getEnumConstantsFor((Class<E>) this.clazz);
    }

    @Nullable
    static <T extends Enum> T[] getEnumConstantsFor(@NotNull Class<T> clazz) {
        try {
            return clazz.getEnumConstants();
        } catch (ExceptionInInitializerError | NoClassDefFoundError ex) {
            log.warn("Failed to load enum \"{}\"; reason: access encountered initializer error {}",
                clazz.getName(), (ex.getCause() == null ? ex : ex.getCause()).toString()
            );

            return null;
        }
    }

    private static class ClassExpression extends Expression {
        private final List<Expression> children;

        ClassExpression(Expression methods, Expression fields, Expression constructors, Expression innerClasses, Expression clazzHeader) {
            children = Collections.unmodifiableList(Arrays.asList(
                clazzHeader, Expressions.blockWith(fields, constructors, methods, innerClasses)
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

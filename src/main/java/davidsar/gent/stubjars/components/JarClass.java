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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JarClass<T> extends JarModifiers implements CompileableExpression {
    private static final Logger log = LoggerFactory.getLogger(JarClass.class);
    private final static Pattern classEntryPatternToBeStripped = Pattern.compile("\\.class$");
    private static Map<Class<?>, JarClass<?>> classToJarClassMap;
    private final Class<T> klazz;
    private Set<JarConstructor> constructors;
    private Set<JarMethod> methods;
    private Set<JarClass<?>> innerClasses;

    public JarClass(@NotNull ClassLoader classLoader, @NotNull String entryName) throws ClassNotFoundException {
        String convertedName = convertEntryNameToClassName(entryName);
        //noinspection unchecked
        klazz = (Class<T>) Class.forName(convertedName, false, classLoader);
    }

    private JarClass(@NotNull Class<T> klazz) {
        this.klazz = klazz;
    }

    public static void loadClassToJarClassMap(@NotNull Map<Class<?>, @NotNull JarClass<?>> map) {
        classToJarClassMap = map;
    }

    @NotNull
    static <T> JarClass<?> forClass(@NotNull Class<T> klazz) {
        if (classToJarClassMap != null && classToJarClassMap.containsKey(klazz)) return classToJarClassMap.get(klazz);

        return new JarClass<>(klazz);
    }

    @NotNull
    private static String convertEntryNameToClassName(@NotNull String entryName) {
        Matcher matcher = classEntryPatternToBeStripped.matcher(entryName);
        if (matcher.find()) entryName = matcher.replaceAll(Constants.EMPTY_STRING);
        return entryName.replace('/', '.');
    }

    boolean isEnum() {
        return klazz.isEnum();
    }

    @NotNull
    public Class<T> getKlazz() {
        return klazz;
    }

    public boolean isInnerClass() {
        return klazz.getDeclaringClass() != null || klazz.isLocalClass() || klazz.isAnonymousClass();
    }

    @NotNull
    public String name() {
        return klazz.getSimpleName();
    }

    static boolean hasSafeName(@NotNull Class<?> klazz) {
        JarClass jarClass = JarClass.forClass(klazz);
        return !jarClass.klazz.isSynthetic() && !jarClass.klazz.isAnonymousClass();
    }

    @NotNull
    static String safeFullNameForClass(@NotNull Class<?> klazz) {
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
    private Class<?>[] implementsInterfaces() {
        return klazz.getInterfaces();
    }

    @NotNull
    private Type[] implementsGenericInterfaces() {
        return klazz.getGenericInterfaces();
    }

    boolean isInterface() {
        return klazz.isInterface();
    }

    boolean isAnnotation() {
        return klazz.isAnnotation();
    }

    @Override
    protected int getModifiers() {
        return klazz.getModifiers();
    }

    private Set<JarField> fields() {
        return Arrays.stream(klazz.getDeclaredFields())
                .map(field -> new JarField(this, field))
                .filter(field -> field.security() != SecurityModifier.PRIVATE)
                .collect(Collectors.toSet());
    }

    @NotNull
    public Set<JarClass<?>> innerClasses() {
        if (innerClasses == null) {
            innerClasses = Arrays.stream(klazz.getDeclaredClasses())
                    .filter(klazz -> !klazz.isLocalClass())
                    .filter(klazz -> !klazz.isAnonymousClass())
                    .map(JarClass::forClass).collect(Collectors.toSet());
        }
        return innerClasses;
    }

    @NotNull
    private Set<JarMethod> methods() {
        if (methods == null) {
            methods = Arrays.stream(klazz.getDeclaredMethods())
                    .map(method1 -> new JarMethod(this, method1))
                    .filter(method -> method.security() != SecurityModifier.PRIVATE)
                    .filter(method -> !method.isSynthetic())
                    .filter(JarMethod::shouldIncludeStaticMethod)
                    .collect(Collectors.toSet());
        }
        return methods;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JarClass && klazz.equals(((JarClass) o).klazz);
    }

    @Override
    public int hashCode() {
        return klazz.hashCode();
    }

    @NotNull Set<JarClass> allSuperClassesAndInterfaces() {
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
        if (superClass == null)
            return superClasses;
        superClasses.add(superClass);
        return allSuperClasses(superClass, superClasses);
    }

    boolean hasMethod(@NotNull Method method) {
        try {
            klazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @NotNull
    private Type extendsGenericClass() {
        return klazz.getGenericSuperclass();
    }

    @NotNull Set<JarConstructor> constructors() {
        //noinspection unchecked
        if (constructors == null) {
            //noinspection unchecked
            constructors = Arrays.stream(klazz.getDeclaredConstructors())
                    .map(x -> new JarConstructor(this, x))
                    .filter(JarConstructor::shouldIncludeCotr)
                    .collect(Collectors.toSet());

            constructors.stream()
                    .filter(JarConstructor::canRewriteConstructorParams).findAny()
                    .ifPresent(jarConstructor -> constructors = Stream.concat(
                            constructors.stream()
                                    .filter(cotr -> !(cotr.canRewriteConstructorParams() || cotr.parameters().length == 0)),
                            Stream.of(jarConstructor)
                    ).collect(Collectors.toSet()));
        }

        return constructors;
    }

    @Override
    public Expression compileToExpression() {
        return compileClass(false, null);
    }

    @NotNull
    private Expression compileClass(boolean isEnumConstant, String enumName) {
        final String methods = compileMethods(isEnumConstant);
        final String fields = compileFields();
        final Expression klazzHeader;
        final String cotrs;
        final String innerClasses;
        if (isEnumConstant) {
            cotrs = Constants.EMPTY_STRING;
            innerClasses = Constants.EMPTY_STRING;
            klazzHeader = Expression.spaceAfter(enumName);
        } else {
            cotrs = compileCotr();
            innerClasses = compileInnerClasses();
            klazzHeader = compileHeader();
        }

        // Enums need to be handled quite a bit differently, but we also need to check if we are working on
        // an enum constant to prevent infinite recursion
        if (isEnum() && !isEnumConstant) {
            Expression enumMembers = Expression.StringExpression.EMPTY.statement();
            //noinspection unchecked
            Enum[] invokedExpression = getEnumConstants();

            if (invokedExpression != null) {
                enumMembers = new EnumMembers(Arrays.stream(invokedExpression)
                        .map(member -> JarClass.forClass(member.getClass()).compileClass(true, member.name()))
                        .toArray(Expression[]::new)).statement();
            }

            return Expression.of(Expression.of(klazzHeader), Expression.block(enumMembers.toString(), fields, methods, innerClasses));
        }

        return Expression.of(Expression.of(klazzHeader), Expression.block(fields, cotrs, methods, innerClasses));
    }

    @NotNull
    private String compileFields() {
        return this.fields().stream()
                .filter(field ->
                        !(field.getClazz().isEnum() || field.getClazz().isInnerClass()) && !field.isStatic() && !field.isSynthetic())
                .filter(field -> {
                    Class<?> superClazz = field.getClazz().extendsClass();
                    if (superClazz != null) {
                        try {
                            superClazz.getDeclaredField(field.name());
                            return false;
                        } catch (NoSuchFieldException ignored) {

                        }
                    }

                    return true;
                })
                .map(field -> Constants.INDENT + field.compileToExpression() + Constants.NEW_LINE_CHARACTER)
                .collect(Collectors.joining());
    }

    @NotNull
    private String compileMethods(boolean isEnumConstant) {
        String methods = this.methods().stream()
                .map(method -> method.compileToString(isEnumConstant).toString())
                .flatMap(x -> Arrays.stream(x.split(Constants.NEW_LINE_CHARACTER))).collect(Collectors.joining(System.lineSeparator() + Constants.INDENT));

        if (methods.endsWith(Constants.NEW_LINE_CHARACTER)) {
            methods = methods.substring(0, methods.length() - 1);
        }

        return methods;
    }

    @NotNull
    private String compileInnerClasses() {
        Set<JarClass<?>> innerClasses = innerClasses();
        if (innerClasses.size() == 0) return Constants.EMPTY_STRING;
        return innerClasses.stream()
                .map(x -> (Constants.NEW_LINE_CHARACTER + x.compileToExpression() + Constants.NEW_LINE_CHARACTER).split(Constants.NEW_LINE_CHARACTER))
                .flatMap(Arrays::stream)
                .collect(Collectors.joining(System.lineSeparator() + Constants.INDENT));
    }

    @NotNull
    private String compileCotr() {
        // Interfaces don't have constructors
        if (isInterface()) return Constants.EMPTY_STRING;
        return constructors().stream()
                .map(JarConstructor::compileToExpression)
                .flatMap(cotr -> Arrays.stream(cotr.toString().split(Constants.NEW_LINE_CHARACTER)))
                .map(cotr -> Constants.INDENT + cotr)
                .collect(Collectors.joining(Constants.NEW_LINE_CHARACTER));
    }

    @NotNull
    private Expression compileHeader() {
        return new ClassHeaderExpression();
    }

    @NotNull
    private String compileHeaderImplements() {
        StringBuilder implementsS = new StringBuilder();
        if (implementsInterfaces().length > 0 && !(isAnnotation() && implementsInterfaces().length == 1)) {
            implementsS = klazz.isInterface() ? new StringBuilder("extends ") : new StringBuilder("implements ");
            implementsS.append(Utils.arrayToCommaSeparatedList(implementsGenericInterfaces(), x -> {
                if (x.equals(Annotation.class)) return null;

                return JarType.toString(x);
            }));
            implementsS.append(Constants.SPACE);
        }
        return implementsS.toString();
    }

    @NotNull
    private String compileHeaderExtends() {
        final String extendsS;
        Class<?> extendsClazz = extendsClass();
        if (extendsClazz != null && !(extendsClazz.equals(Enum.class))) {
            extendsS = "extends " + JarType.toString(extendsGenericClass()) + Constants.SPACE;
        } else {
            extendsS = Constants.EMPTY_STRING;
        }
        return extendsS;
    }

    private String compileTypeParameters() {
        final String genericS;
        TypeVariable<? extends Class<?>>[] typeParameters = getKlazz().getTypeParameters();
        genericS = JarType.convertTypeParametersToString(typeParameters);
        return genericS;
    }

    @NotNull
    private Expression compileHeaderAnnotation() {
        final Expression annotationS;
        if (isAnnotation() && getKlazz().isAnnotationPresent(Retention.class)) {
            RetentionPolicy retentionPolicy = getKlazz().getAnnotation(Retention.class).value();
            annotationS = Expression.of(
                    Expression.StringExpression.AT,
                    Expression.forType(Retention.class, JarClass.safeFullNameForClass(Retention.class)),
                    Expression.parenthetical(Expression.of(
                            Expression.of(safeFullNameForClass(RetentionPolicy.class)),
                            Expression.StringExpression.PERIOD,
                            Expression.of(retentionPolicy.name()))),
                    Expression.StringExpression.SPACE
            );
        } else {
            annotationS = Expression.StringExpression.EMPTY;
        }
        return annotationS;
    }

    @NotNull
    private static String typeString(@NotNull JarClass<?> klazz, boolean enumTypeClass) {
        final String typeS;
        if (klazz.isAnnotation()) {
            typeS = "@interface ";
        } else if (klazz.isInterface()) {
            typeS = "interface ";
        } else if (enumTypeClass) {
            typeS = "enum ";
        } else {
            typeS = "class ";
        }
        return typeS;
    }

    private <ENUM_CLASS extends Enum> ENUM_CLASS[] getEnumConstants() {
        if (!isEnum())
            throw new IllegalArgumentException("Not an enum");
        //noinspection unchecked
        return JarClass.getEnumConstantsFor((Class<ENUM_CLASS>) this.klazz);
    }

    @Nullable
    static <T extends Enum> T[] getEnumConstantsFor(@NotNull Class<T> klazz) {
        T[] invokedExpression = null;
        try {
            Method values = klazz.getMethod("values");
            values.setAccessible(true);
            //noinspection unchecked
            invokedExpression = (T[]) values.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | ExceptionInInitializerError | NoClassDefFoundError e) {
            log.warn("Failed to load enum \"{}\"; reason: access encountered {}", klazz.getName(), e.toString());
        } catch (InvocationTargetException ex) {
            log.warn("Failed to load enum \"{}\"; reason: loading encountered {}", klazz.getName(), ex.getTargetException().toString());
        }

        return invokedExpression;
    }

    private static class EnumMembers extends ListExpression {
        EnumMembers(Expression[] expressions) {
            super(expressions, ListExpression.DELIMITER_COMMA_NEW_LINE);
        }
    }

    private static class ListExpression extends Expression {
        static Expression[] DELIMITER_COMMA_NEW_LINE = new Expression[]{StringExpression.COMMA, StringExpression.NEW_LINE};
        public static Expression[] DELIMITER_COMMA_SPACE = new Expression[]{StringExpression.COMMA, StringExpression.SPACE};
        private final Expression[] expressions;
        private final Expression[] delimiters;
        private List<Expression> children;

        private ListExpression(Expression[] expressions, Expression[] delimiters) {
            this.expressions = expressions;
            this.delimiters = delimiters;
        }

        @Override
        public boolean hasChildren() {
            return true;
        }

        @Override
        public List<Expression> children() {
            if (children == null) {
                children = buildChildrenList();
            }

            return children;
        }

        @NotNull
        private List<Expression> buildChildrenList() {
            List<Expression> expressionChildren = new ArrayList<>(expressions.length * (delimiters.length + 1) - delimiters.length);
            for (int iExpression = 0; iExpression < expressions.length; iExpression++) {
                expressionChildren.add(expressions[iExpression]);
                if (iExpression < expressions.length - 1) {
                    expressionChildren.addAll(Arrays.asList(delimiters));
                }
            }

            return expressionChildren;
        }
    }

    private class ClassHeaderExpression extends Expression {
        private final Expression annotationS;
        private final Expression security;
        private final Expression staticS;
        private final Expression abstractS;
        private final Expression finalS;
        private final Expression typeS;
        private final Expression nameS;
        private final Expression genericS;
        private final Expression extendsS;
        private final Expression implementsS;

        private ClassHeaderExpression() {
            this.annotationS = compileHeaderAnnotation();
            this.security = security().expression();
            this.finalS = Expression.whenWithSpace(isFinal() && !isEnum(), "final");
            this.staticS = Expression.whenWithSpace(isStatic() && !isEnum(), "static");
            this.abstractS = Expression.whenWithSpace(isAbstract() && !isEnum() && !isAnnotation(), "abstract");
            this.typeS = Expression.forType(getKlazz(), typeString(JarClass.this, isEnum()));
            this.genericS = Expression.of(compileTypeParameters());
            this.nameS = Expression.of(name());
            this.extendsS = Expression.of(compileHeaderExtends());
            this.implementsS = Expression.of(compileHeaderImplements());
        }

        @Override
        public boolean hasChildren() {
            return true;
        }

        @Override
        public List<Expression> children() {
            return Arrays.asList(annotationS, security, staticS, abstractS, finalS, typeS, nameS, genericS, extendsS, implementsS);
        }
    }
}

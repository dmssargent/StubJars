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

package me.davidsargent.stubjars.components.writer;

import me.davidsargent.stubjars.Preconditions;
import me.davidsargent.stubjars.Utils;
import me.davidsargent.stubjars.components.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class JavaClassWriter extends Writer {
    private static final String EMPTY_STRING = "";
    private static final String INDENT = "    ";
    private final JarClass<?> klazz;
    private String compiledString;

    public JavaClassWriter(@NotNull final File file, @NotNull final JarClass<?> klazz, @NotNull WriterThread writerThread) {
        super(file, writerThread);
        Preconditions.checkNotNull(file);
        Preconditions.checkNotNull(klazz);
        this.klazz = klazz;
    }

    private String compile() {
        if (compiledString == null) {
            String compiledStringA = compile(klazz);
            this.compiledString = compiledStringA
                    .replaceAll("(\\s*\\n )+\\s*\\n(\\s*)", "\n\n$2")
                    .replaceAll("([^ \\t\\n])[\\t ]+([^ \\t])", "$1 $2");
        }

        return compiledString;
    }

    @NotNull
    private static String compile(@NotNull final JarClass<?> klazz) {
        String packageStatement = compilePackageStatement(klazz);
        String classBody = compileClass(klazz);
        return String.format("%s\n%s", packageStatement, classBody);
    }

    private static String compileClass(@NotNull final JarClass<?> klazz) {
        return compileClass(klazz, false, null);
    }

    @NotNull
    private static String compileClass(@NotNull final JarClass<?> klazz, boolean isEnumConstant, String enumName) {
        final String methods = compileMethods(klazz, isEnumConstant);
        final String fields = compileFields(klazz);
        final String klazzHeader;
        final String cotrs;
        final String innerClasses;
        if (isEnumConstant) {
            cotrs = "";
            innerClasses = "";
            klazzHeader = enumName + " ";
        } else {
            cotrs = compileCotr(klazz);
            innerClasses = compileInnerClasses(klazz);
            klazzHeader = compileHeader(klazz);
        }

        // Enums need to be handled quite a bit differently, but we also need to check if we are working on
        // an enum constant to prevent infinite recursion
        if (klazz.isEnum() && !isEnumConstant) {
            String enumMembers = EMPTY_STRING;
            try {
                Method values = klazz.getKlazz().getMethod("values");
                values.setAccessible(true);
                enumMembers = Arrays.stream((Enum[]) values.invoke(null))
                        .map(member -> compileClass(JarClass.forClass(member.getClass()), true, member.name()))
                        .collect(Collectors.joining("," + System.lineSeparator()));
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return String.format("%s{\n%s\n;%s\n%s\n%s\n}", klazzHeader, enumMembers, fields, methods, innerClasses);
        }

        return String.format("%s{\n%s\n%s\n%s\n%s\n}", klazzHeader, fields, cotrs, methods, innerClasses);
    }

    /**
     * Produces a String containing a source code version of the package name declaration
     *
     * @param klazz the {@link JarClass} to create the declaration for
     * @return source code version of the package name declaration
     */
    @NotNull
    private static String compilePackageStatement(@NotNull final JarClass<?> klazz) {
        return String.format("package %s;\n", klazz.packageName());
    }

    @NotNull
    private static String compileHeader(@NotNull final JarClass<?> klazz) {
        final boolean enumTypeClass = klazz.isEnum();
        final String security = klazz.security().getModifier() + (klazz.security() == SecurityModifier.PACKAGE ? EMPTY_STRING : " ");
        final String finalS = klazz.isFinal() && !enumTypeClass ? "final " : EMPTY_STRING;
        final String staticS = klazz.isStatic() && !enumTypeClass ? "static " : EMPTY_STRING;
        final String abstractS = klazz.isAbstract() && !enumTypeClass ? "abstract " : EMPTY_STRING;

        final String typeS;
        if (klazz.isInterface()) {
            typeS = "interface ";
        } else if (klazz.isAnnotation()) {
            typeS = "@interface ";
        } else if (enumTypeClass) {
            typeS = "enum ";
        } else {
            typeS = "class ";
        }

        final String genericS;
        TypeVariable<? extends Class<?>>[] typeParameters = klazz.getKlazz().getTypeParameters();
        genericS = convertTypeParametersToString(typeParameters);

        final String nameS = klazz.name();
        final String extendsS;
        Class<?> extendsClazz = klazz.extendsClass();
        if (extendsClazz != null && !extendsClazz.equals(Enum.class)) {
            extendsS = "extends " + JarType.toString(klazz.extendsGenericClass()) + " ";
        } else {
            extendsS = EMPTY_STRING;
        }

        StringBuilder implementsS = new StringBuilder();
        if (klazz.implementsInterfaces().length > 0) {
            implementsS = klazz.isInterface() ? new StringBuilder("extends ") : new StringBuilder("implements ");
            implementsS.append(Utils.arrayToCommaSeparatedList(klazz.implementsGenericInterfaces(), JarType::toString));
            implementsS.append(" ");
        }

        return String.format("%s%s%s%s%s%s%s%s%s", security, staticS, abstractS, finalS, typeS, nameS, genericS, extendsS, implementsS);
    }

    @NotNull
    private static String compileCotr(@NotNull final JarClass<?> klazz) {
        // Interfaces don't have constructors
        if (klazz.isInterface()) return EMPTY_STRING;

        final Set<JarConstructor<?>> cotrs = klazz.constructors();
        StringBuilder compiledCotr = new StringBuilder();
        for (JarConstructor<?> cotr : cotrs) {
            // If any of these conditions are meet, we shouldn't need to declare a constructor
            if (cotr.isDefaultConstructor() &&
                    (cotr.getConstructor().getDeclaringClass().getSuperclass() == null ||
                            JarConstructor.hasDefaultConstructor(cotr.getConstructor().getDeclaringClass().getSuperclass())))
                continue;

            // Figure out correct method signature
            final String security;
            if (klazz.isInterface())
                security = EMPTY_STRING;
            else
                security = cotr.security().getModifier() + (cotr.security() == SecurityModifier.PACKAGE ? EMPTY_STRING : " ");
            final String nameS = cotr.name();
            final String parametersS = Utils.arrayToCommaSeparatedList(cotr.parameters(), x -> x);

            final String stubMethod;
            Class<?> klazzSuperClass = klazz.extendsClass();

            // What should the contents of the constructor be?
            if (klazzSuperClass == null || JarConstructor.hasDefaultConstructor(klazzSuperClass)) {
                stubMethod = EMPTY_STRING;
            } else {
                // We need to call some form of the default constructor, so we can compile code
                JarConstructor<?>[] declaredConstructors;
                declaredConstructors = (JarConstructor<?>[]) JarClass.forClass(klazzSuperClass).constructors().toArray(new JarConstructor<?>[0]);
                if (declaredConstructors.length > 0) {
                    stubMethod = INDENT + "super(" + Utils.arrayToCommaSeparatedList(
                            declaredConstructors[0].getConstructor().getGenericParameterTypes(),
                            paramType ->
                                    "(" + JarType.toString(paramType)  + ") " + JavaClassWriter.defaultValueForType(paramType)
                    ) + ");";
                } else {
                    throw new UnsupportedOperationException("Cannot infer super cotr to write for " + klazz.getKlazz().getName());
                }
            }

            compiledCotr.append('\n').append(security).append(nameS).append('(').append(parametersS).append(')');
            compiledCotr.append(" {\n").append(stubMethod).append("\n}\n\n");
        }

        return compiledCotr.toString();
    }

    /**
     * Returns a String contains the default value for a given type
     *
     * @param type a {@link Type} to get the default value for
     * @return a String with the default type for the parameter type
     */
    private static String defaultValueForType(Type type) {
        if (!(type instanceof Class)) {
            if (type instanceof ParameterizedType) return defaultValueForType(((ParameterizedType) type).getRawType());
            return defaultValueForType(Object.class);
        }

        if (type.equals(int.class)) {
            return "0";
        } else if (type.equals(double.class)) {
            return "0.0";
        } else if (type.equals(long.class)) {
            return "0L";
        } else if (type.equals(byte.class)) {
            return "(byte) 0";
        } else if (type.equals(short.class)) {
            return "(short) 0";
        } else if (type.equals(boolean.class)) {
            return "false";
        } else if (type.equals(float.class)) {
            return "(float) 0";
        } else if (type.equals(char.class)) {
            return "(char) 0";
        } else {
            return "null";
        }
    }

    @NotNull
    private static String compileMethods(@NotNull final JarClass<?> klazz, boolean isEnumField) {
        final Set<JarMethod> methods = klazz.methods();
        StringBuilder compiledMethods = new StringBuilder();
        for (JarMethod method : methods) {
            // Skip create methods for these types of things, enum fields can't have static methods
            if ((isEnumField || klazz.isInterface()) && method.isStatic()) continue;
            // Check if the enum method we are about to write could actually exist
            if (isEnumField) {
                // todo: fix this issue since Enum members may have static classes
                if (method.isFinal())
                    continue;
                Class<?> declaringClass = klazz.getKlazz().getDeclaringClass();
                if (declaringClass != null) {
                    try {
                        declaringClass.getDeclaredMethod(method.name(), method.parameterTypes());
                        continue;
                    } catch (NoSuchMethodException ignored) { }
                }
            }

            // Figure method signature
            final String security;
            if (klazz.isInterface())
                security = EMPTY_STRING;
            else
                security = method.security().getModifier() + (method.security() == SecurityModifier.PACKAGE ? EMPTY_STRING : " ");
            final String finalS = method.isFinal() ? "final " : EMPTY_STRING;
            final String staticS = method.isStatic() ? "static " : EMPTY_STRING;
            final String abstractS;
            if (klazz.isInterface())
                abstractS = EMPTY_STRING;
            else
                abstractS = method.isAbstract() ? "abstract " : EMPTY_STRING;
            final String returnTypeS = JarType.toString(method.genericReturnType());
            final String nameS = method.name();
            final String parametersS = Utils.arrayToCommaSeparatedList(method.parameters(), x -> x);

            final String genericS;
            TypeVariable<Method>[] typeParameters = method.typeParameters();
            genericS = convertTypeParametersToString(typeParameters);

            // What should the method body be?
            final String stubMethod;
            final Class<?> returnType = method.returnType();
            if (returnType.equals(void.class)) {
                stubMethod = EMPTY_STRING;
            } else {
                stubMethod = INDENT + "return " + defaultValueForType(returnType) + ";";
            }

            // Finally, put all of the pieces together
            compiledMethods.append('\n').append(security).append(finalS).append(staticS).append(abstractS).append(genericS).append(returnTypeS).append(" ")
                    .append(nameS).append('(').append(parametersS).append(')');
            if (method.isAbstract() || (klazz.isInterface() && !method.isStatic())) {
                compiledMethods.append(";");
            } else {
                compiledMethods.append(" {\n").append(stubMethod).append("\n}\n\n");
            }

        }

        if (compiledMethods.length() > 2 && compiledMethods.lastIndexOf("\n") == compiledMethods.length() - 1) {
            compiledMethods.replace(compiledMethods.length() - 2, compiledMethods.length(), EMPTY_STRING);
        }

        // Indent all of the methods we created
        return Arrays.stream(compiledMethods.toString().split("\n"))
                .collect(Collectors.joining(System.lineSeparator() + INDENT));
    }

    private static String convertTypeParametersToString(TypeVariable<?>[] typeParameters) {
        String genericS;
        if (typeParameters.length == 0) {
            genericS = " ";
        } else {
            String typeParams = Utils.arrayToCommaSeparatedList(typeParameters, typeParam -> {
                if (typeParam.getBounds()[0] == Object.class) {
                    return typeParam.getName();
                } else {
                    return typeParam.getName() + " extends " + JarType.toString(typeParam.getBounds()[0]);
                }
            });
            genericS = "<" + typeParams + "> ";
        }

        return genericS;
    }

    @NotNull
    private static String compileFields(@NotNull final JarClass<?> klazz) {
        return EMPTY_STRING;
    }

    @NotNull
    private static String compileInnerClasses(@NotNull final JarClass<?> klazz) {
        @NotNull JarClass[] innerClasses = klazz.innerClasses();
        if (innerClasses.length == 0) return EMPTY_STRING;
        return Arrays.stream(innerClasses)
                .map(x -> ("\n" + compileClass(x) + "\n").split("\n"))
                .flatMap(Arrays::stream)
                .collect(Collectors.joining(System.lineSeparator() + INDENT));
    }

    public void write() {
        writeDataWithDedicatedThread(compile());
    }
}

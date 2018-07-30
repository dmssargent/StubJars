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

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Set;

public class JarMethod extends JarModifiers implements CompileableExpression {
    private final JarClass parentClazz;
    private final Method method;
    private String[] cachedParameters;

    JarMethod(@NotNull JarClass parentClazz, @NotNull Method method) {
        this.parentClazz = parentClazz;
        this.method = method;
    }

    @Override
    protected int getModifiers() {
        return method.getModifiers();
    }

    private String name() {
        return method.getName();
    }

    @NotNull
    private String[] parameters() {
        if (cachedParameters != null) return cachedParameters;
        Parameter[] parameters = method.getParameters();
        String[] stringifiedParameters = Arrays.stream(parameters)
                .map(parameter -> JarType.toString(parameter.getParameterizedType()) + Constants.SPACE + parameter.getName())
                .toArray(String[]::new);

        if (method.isVarArgs()) {
            Parameter varArgsParameter = parameters[parameters.length - 1];
            Type parameterizedType = varArgsParameter.getParameterizedType();
            if (JarType.isArray(parameterizedType)) {
                if (parameterizedType instanceof GenericArrayType) {
                    stringifiedParameters[parameters.length - 1] = JarType.toString(((GenericArrayType) parameterizedType).getGenericComponentType()) + "... " + varArgsParameter.getName();
                } else if (parameterizedType instanceof Class) {
                    stringifiedParameters[parameters.length - 1] = JarType.toString(((Class) parameterizedType).getComponentType()) + "... " + varArgsParameter.getName();
                }
            }
        }

        cachedParameters = stringifiedParameters;
        return stringifiedParameters;
    }

    private Expression buildMethod(boolean isEnumField) {
        // Skip create methods for these types of things, enum fields can't have static methods
        if ((isEnumField || parentClazz.isInterface()) && isStatic()) return Expression.StringExpression.EMPTY;
        // Check if the enum method we are about to write could actually exist
        if (isEnumField) {
            // todo: fix this issue since Enum members may have static classes
            if (isFinal())
                return Expression.StringExpression.EMPTY;
            Class<?> declaringClass = parentClazz.getKlazz().getDeclaringClass();
            if (declaringClass != null) {
                try {
                    declaringClass.getDeclaredMethod(name(), parameterTypes());
                    return Expression.StringExpression.EMPTY;
                } catch (NoSuchMethodException ignored) {
                }
            }
        }

        // Figure method signature
        final Expression security;
        if (parentClazz.isInterface()) {
            security = Expression.StringExpression.EMPTY;
        } else {
            security = Expression.of(Expression.of(security().getModifier()), Expression.whenWithSpace(security() != SecurityModifier.PACKAGE, Constants.SPACE));
        }
        final Expression finalS = Expression.whenWithSpace(isFinal(), "final");
        final Expression staticS = Expression.whenWithSpace(isStatic(), "static");
        final Expression abstractS;
        if (parentClazz.isInterface()) {
            abstractS = Expression.StringExpression.EMPTY;
        } else {
            abstractS = Expression.whenWithSpace(isAbstract(), "abstract");
        }
        final Expression returnTypeS = Expression.forType(genericReturnType(), JarType.toString(genericReturnType()));
        final Expression nameS = Expression.of(name());
        final Expression parametersS = Expression.of(Utils.arrayToCommaSeparatedList(parameters(), x -> x));
        final Expression throwsS = Expression.of(requiresThrowsSignature() ? " throws " + Utils.arrayToCommaSeparatedList(throwsTypes(), JarType::toString) : Constants.EMPTY_STRING);
        final Expression genericS;
        TypeVariable<Method>[] typeParameters = typeParameters();
        genericS = Expression.of(JarType.convertTypeParametersToString(typeParameters));

        // What should the method body be?
        final Expression stubMethod;
        final Type returnType = genericReturnType();
        if (returnType.equals(void.class)) {
            stubMethod = Expression.block();
        } else {
            stubMethod = Expression.block(Expression.of(Expression.spaceAfter("return"), Expression.forType(returnType, JarConstructor.castedDefaultType(returnType, parentClazz))).statement());
        }

        // Finally, put all of the pieces together
        Expression methodHeader = Expression.of(Expression.StringExpression.NEW_LINE, security, finalS, staticS, abstractS, genericS, returnTypeS, Expression.StringExpression.SPACE, nameS, Expression.parenthetical(parametersS), throwsS);
        Expression methodBody;
        if (parentClazz.isAnnotation() && hasDefaultValue()) {
            methodBody = Expression.of(Expression.of(" default "), Expression.forType(defaultValue().getClass(), Value.defaultValueForType(defaultValue().getClass(), true)), Expression.StringExpression.SEMICOLON);
        } else if (isAbstract() || (parentClazz.isInterface() && !isStatic())) {
            methodBody = Expression.StringExpression.SEMICOLON;
        } else {
            methodBody = stubMethod;
        }

        return Expression.of(methodHeader, Expression.StringExpression.SPACE, methodBody);
    }

    boolean isSynthetic() {
        return method.isSynthetic();
    }

    boolean shouldIncludeStaticMethod() {
        if (!isStatic()) return shouldIncludeMethod();
        JarClass<?> klazz = JarClass.forClass(method.getDeclaringClass());
        if (klazz.isEnum()) {
            String name = method.getName();
            if (name.equals("values") || name.equals("valueOf")) {
                return false;
            }
        }

        Set<JarClass> jarClasses = klazz.allSuperClassesAndInterfaces();
        long count = jarClasses.stream()
                .filter(x -> x.hasMethod(method))
                .count();
        return count == 0 && shouldIncludeMethod();
    }

    private boolean shouldIncludeMethod() {
        for (Class<?> paramType : method.getParameterTypes()) {
            if (!JarClass.hasSafeName(paramType)) return false;
        }

        return JarClass.hasSafeName(method.getReturnType());
    }

    private Type genericReturnType() {
        return method.getGenericReturnType();
    }

    private TypeVariable<Method>[] typeParameters() {
        return method.getTypeParameters();
    }

    private Class<?>[] parameterTypes() {
        return method.getParameterTypes();
    }

    private boolean hasDefaultValue() {
        return defaultValue() != null;
    }

    private Object defaultValue() {
        return method.getDefaultValue();
    }

    private Type[] throwsTypes() {
        return method.getGenericExceptionTypes();
    }

    private boolean requiresThrowsSignature() {
        return throwsTypes().length > 0;
    }

    @Override
    public Expression compileToExpression() {
        return buildMethod(false);
    }

    Expression compileToString(boolean isEnumField) {
        return buildMethod(isEnumField);
    }
}

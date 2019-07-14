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

import davidsar.gent.stubjars.components.expressions.CompileableExpression;
import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import davidsar.gent.stubjars.components.expressions.MethodDeclarationExpression;
import davidsar.gent.stubjars.components.expressions.StringExpression;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Set;

public class JarMethod extends JarModifiers implements CompileableExpression {
    private static final Logger log = LoggerFactory.getLogger(JarMethod.class);
    private final JarClass<?> parentClazz;
    private final Method method;
    private Expression[] cachedParameters;

    JarMethod(@NotNull JarClass<?> parentClazz, @NotNull Method method) {
        this.parentClazz = parentClazz;
        this.method = method;
    }

    @Override
    protected int getModifiers() {
        return method.getModifiers();
    }

    public String name() {
        return method.getName();
    }

    @NotNull
    public Expression[] parameters() {
        if (cachedParameters != null) {
            return cachedParameters;
        }
        Parameter[] parameters = method.getParameters();
        Expression[] stringifiedParameters = Arrays.stream(parameters)
            .map(parameter -> Expressions.of(JarType.toExpression(
                parameter.getParameterizedType(), getParentClazz()), StringExpression.SPACE, Expressions.fromString(parameter.getName()))
            ).toArray(Expression[]::new);

        if (method.isVarArgs()) {
            Parameter varArgsParameter = parameters[parameters.length - 1];
            Type parameterizedType = varArgsParameter.getParameterizedType();
            if (JarType.isArray(parameterizedType)) {
                if (parameterizedType instanceof GenericArrayType) {
                    stringifiedParameters[parameters.length - 1] =
                        Expressions.of(JarType.toExpression(
                            ((GenericArrayType) parameterizedType).getGenericComponentType(), getParentClazz()
                        ), StringExpression.VARARGS, StringExpression.SPACE, Expressions.fromString(varArgsParameter.getName()));
                } else if (parameterizedType instanceof Class) {
                    stringifiedParameters[parameters.length - 1] =
                        Expressions.of(JarType.toExpression(((Class) parameterizedType).getComponentType(), getParentClazz()
                        ), StringExpression.VARARGS, StringExpression.SPACE, Expressions.fromString(varArgsParameter.getName()));
                }
            }
        }

        cachedParameters = stringifiedParameters;
        return stringifiedParameters;
    }

    private Expression buildMethod(boolean isEnumField) {
        if (!shouldWriteMethod(isEnumField)) {
            return StringExpression.EMPTY;
        }

        Expression methodDeclaration = MethodDeclarationExpression.from(this, isEnumField).getFormattedString();

        // What should the method body be?
        final Expression stubMethod;
        final Type returnType = genericReturnType();
        if (returnType.equals(void.class)) {
            stubMethod = Expressions.emptyBlock();
        } else {
            stubMethod = Expressions
                .blockWith(Expressions.of(
                    StringExpression.RETURN,
                    StringExpression.SPACE,
                    Expressions.forType(
                        returnType, JarConstructor.castedDefaultType(returnType, parentClazz))
                ).asStatement());
        }

        if (getParentClazz().isAnnotation() && hasDefaultValue()) {
            Expression methodBody = Expressions.of(
                Expressions.toSpaceAfter("default"),
                Expressions.forType(
                    defaultValue().getClass(), Value.defaultValueForType(defaultValue().getClass(),
                        getParentClazz(), true)
                ),
                StringExpression.SEMICOLON
            );

            return Expressions.of(methodDeclaration, StringExpression.SPACE, methodBody);
        } else if ((isAbstract() && !isEnumField) || (getParentClazz().isInterface() && !isStatic())) {
            return methodDeclaration.asStatement();
        }

        return Expressions.of(methodDeclaration, StringExpression.SPACE, stubMethod);
    }

    private boolean shouldWriteMethod(boolean isEnumField) {
        // Skip create methods for these types of things, enum fields can't have static methods
        if ((isEnumField || getParentClazz().isInterface()) && isStatic()) {
            return false;
        }

        // Check if the enum method we are about to write could actually exist
        if (!isEnumField) {
            return true;
        }

        if (isFinal()) {
            return false;
        }

        Class<?> declaringClass = getParentClazz().getClazz().getDeclaringClass();
        if (declaringClass == null) {
            return true;
        }

        try {
            declaringClass.getDeclaredMethod(name(), parameterTypes());
            return false;
        } catch (NoSuchMethodException ignored) {
            // log.debug("method \"{}\" does not exist on enum \"{}\"", name(), parentClazz.name());
        }
        return true;
    }

    boolean isSynthetic() {
        return method.isSynthetic();
    }

    boolean shouldIncludeStaticMethod() {
        if (!isStatic()) {
            return shouldIncludeMethod();
        }

        if (getParentClazz().isEnum()) {
            if (name().equals("values") || name().equals("valueOf")) {
                return false;
            }
        }

        Set<JarClass> jarClasses = getParentClazz().allSuperClassesAndInterfaces();
        long count = jarClasses.stream()
                .filter(x -> x.hasMethod(method))
                .count();
        return count == 0 && shouldIncludeMethod();
    }

    private boolean shouldIncludeMethod() {
        for (Class<?> paramType : method.getParameterTypes()) {
            if (!JarClass.hasSafeName(paramType)) {
                return false;
            }
        }

        return JarClass.hasSafeName(method.getReturnType());
    }

    public Type genericReturnType() {
        return method.getGenericReturnType();
    }

    public TypeVariable<Method>[] typeParameters() {
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

    public Type[] throwsTypes() {
        return method.getGenericExceptionTypes();
    }

    public boolean requiresThrowsSignature() {
        return throwsTypes().length > 0;
    }

    @Override
    public Expression compileToExpression() {
        return buildMethod(false);
    }

    Expression compileToExpression(boolean isEnumField) {
        return buildMethod(isEnumField);
    }

    public JarClass<?> getParentClazz() {
        return parentClazz;
    }

    public AnnotatedElement method() {
        return method;
    }
}

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

import davidsar.gent.stubjars.components.expressions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class JarField extends JarModifiers implements CompileableExpression {
    private static final Logger log = LoggerFactory.getLogger(JarClass.class);
    private final JarClass jarClass;
    private final Field field;

    JarField(JarClass<?> clazz, Field field) {
        this.jarClass = clazz;
        this.field = field;
    }

    @Override
    protected int getModifiers() {
        return field.getModifiers();
    }

    String name() {
        return field.getName();
    }

    private Type genericReturnType() {
        return field.getGenericType();
    }

    @Override
    public Expression compileToExpression() {
        // Figure method signature
        final Expression security = new SecurityModifierExpression(security());
        final Expression finalS = Expressions.whenWithSpace(isFinal(), StringExpression.FINAL);
        final Expression staticS = Expressions.whenWithSpace(isStatic(), StringExpression.STATIC);
        final Expression volatileS = Expressions.whenWithSpace(isVolatile(), StringExpression.VOLATILE);
        final Expression transientS = Expressions.whenWithSpace(isTransient(), StringExpression.TRANSIENT);
        final Expression returnTypeS = JarType.toExpression(genericReturnType(), getClazz());
        final Expression nameS = Expressions.fromString(name());

        final Expression assignmentS;
        if (isFinal() || isStatic()) {
            assignmentS = Expressions.of(
                Expressions.fromString(" = "),
                Expressions.forType(
                    genericReturnType(),
                    determineValueOfField()
                )
            );
        } else {
            assignmentS = StringExpression.EMPTY;
        }

        return Expressions.of(
            Expressions.of(security, finalS, staticS, volatileS, transientS, returnTypeS).asSpaceAfter(),
            nameS, assignmentS
        ).asStatement();
    }

    private Expression determineValueOfField() {
        if (isStatic() && !(jarClass.isInnerClass())) {
            try {
                Class<?> expectedType = field.getType();
                if (expectedType.isAssignableFrom(float.class) || expectedType.isAssignableFrom(Float.class) ||
                    expectedType.isAssignableFrom(double.class) || expectedType.isAssignableFrom(Double.class) ||
                    expectedType.isAssignableFrom(byte.class) || expectedType.isAssignableFrom(Byte.class) ||
                    expectedType.isAssignableFrom(short.class) || expectedType.isAssignableFrom(Short.class) ||
                    expectedType.isAssignableFrom(boolean.class) || expectedType.isAssignableFrom(Boolean.class) ||
                    expectedType.isAssignableFrom(long.class) || expectedType.isAssignableFrom(Long.class) ||
                    expectedType.isAssignableFrom(int.class) || expectedType.isAssignableFrom(Integer.class) ||
                    expectedType.isAssignableFrom(String.class)) {
                    field.setAccessible(true);
                    final Object o = field.get(null);
                    return Expressions.fromString(Value.reduceValueToString(expectedType, o));
                }
            } catch (IllegalAccessException | UnsatisfiedLinkError | NoClassDefFoundError | ExceptionInInitializerError e) {
                log.warn("Could not determine the value of the static field \"{}\" from \"{}\". Reason: {}", name(), getClazz().fullName(), e.getMessage());
            } catch (NullPointerException e) {
                log.warn("Could not determine the value of the static field \"{}\" from \"{}\". Reason: Static field is instance field?", name(), getClazz().name());
            }
        }

        return Value.defaultValueForType(genericReturnType(), getClazz());
    }

    JarClass<?> getClazz() {
        return jarClass;
    }

    boolean isSynthetic() {
        return field.isSynthetic();
    }
}

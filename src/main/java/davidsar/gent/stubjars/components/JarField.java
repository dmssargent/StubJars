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

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class JarField extends JarModifiers implements CompileableExpression {
    private final JarClass jarClass;
    private final Field field;

    public JarField(JarClass<?> clazz, Field method) {
        this.jarClass = clazz;
        this.field = method;
    }

    @Override
    protected int getModifiers() {
        return field.getModifiers();
    }

    public String name() {
        return field.getName();
    }

    private Type genericReturnType() {
        return field.getGenericType();
    }

    @Override
    public Expression compileToExpression() {
        // Figure method signature
        final Expression security = Expression.of(Expression.of(security().getModifier()), Expression.when(security() != SecurityModifier.PACKAGE, Expression.StringExpression.SPACE));
        final Expression finalS = Expression.whenWithSpace(isFinal(), "final");
        final Expression staticS = Expression.whenWithSpace(isStatic(), "static");
        final Expression volatileS = Expression.whenWithSpace(isVolatile(), "volatile");
        final Expression transientS = Expression.whenWithSpace(isTransient(), "transient");
        final Expression returnTypeS = Expression.of(JarType.toString(genericReturnType()));
        final Expression nameS = Expression.of(name());

        final Expression assignmentS;
        if (isFinal()) {
            assignmentS = Expression.of(Expression.of(" = "), Expression.forType(genericReturnType(), Value.defaultValueForType(genericReturnType())));
        } else {
            assignmentS = Expression.StringExpression.EMPTY;
        }

        return Expression.of(Expression.of(security, finalS, staticS, volatileS, transientS, returnTypeS).spaceAfter(), nameS, assignmentS).statement();
    }

    public JarClass<?> getClazz() {
        return jarClass;
    }

    public boolean isSynthetic() {
        return field.isSynthetic();
    }
}

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

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class JarField extends JarModifiers implements CompileableExpression {
    private final JarClass jarClass;
    private final Field field;

    JarField(JarClass<?> clazz, Field method) {
        this.jarClass = clazz;
        this.field = method;
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
        if (isFinal()) {
            assignmentS = Expressions.of(
                Expressions.fromString(" = "),
                Expressions.forType(
                    genericReturnType(),
                    Value.defaultValueForType(genericReturnType(), getClazz())
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

    JarClass<?> getClazz() {
        return jarClass;
    }

    boolean isSynthetic() {
        return field.isSynthetic();
    }
}

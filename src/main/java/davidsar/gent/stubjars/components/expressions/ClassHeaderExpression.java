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

package davidsar.gent.stubjars.components.expressions;

import davidsar.gent.stubjars.components.JarClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClassHeaderExpression extends Expression {
    private final List<Expression> children;

    public ClassHeaderExpression(JarClass jarClass) {
        Expression annotationS = jarClass.compileHeaderAnnotation();
        Expression security = jarClass.security().expression();
        Expression finalS = Expressions.whenWithSpace(jarClass.isFinal() && !jarClass.isEnum(),
            StringExpression.FINAL);
        Expression staticS = Expressions.whenWithSpace(jarClass.isStatic() && !jarClass.isEnum(),
            StringExpression.STATIC);
        Expression abstractS = Expressions.whenWithSpace(jarClass.isAbstract()
            && !jarClass.isEnum()
            && !jarClass.isAnnotation()
            && !jarClass.isInterface(), StringExpression.ABSTRACT);
        Expression typeS = JarClass.typeString(jarClass);
        Expression genericS = jarClass.compileTypeParameters();
        Expression nameS = Expressions.fromString(jarClass.name());
        Expression extendsS = jarClass.compileHeaderExtends();
        Expression implementsS = jarClass.compileHeaderImplements();

        children = Collections.unmodifiableList(Arrays.asList(
            annotationS, security, staticS, abstractS, finalS, typeS, StringExpression.SPACE, nameS, genericS, StringExpression.SPACE, extendsS, implementsS
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

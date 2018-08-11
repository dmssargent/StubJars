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
import java.util.List;

public class ClassHeaderExpression extends Expression {
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

    public ClassHeaderExpression(JarClass jarClass) {
        this.annotationS = jarClass.compileHeaderAnnotation();
        this.security = jarClass.security().expression();
        this.finalS = Expressions.whenWithSpace(jarClass.isFinal() && !jarClass.isEnum(), "final");
        this.staticS = Expressions.whenWithSpace(jarClass.isStatic() && !jarClass.isEnum(), "static");
        this.abstractS = Expressions.whenWithSpace(jarClass.isAbstract()
            && !jarClass.isEnum()
            && !jarClass.isAnnotation()
            && !jarClass.isInterface(), "abstract");
        this.typeS = Expressions.forType(jarClass.getClazz(), JarClass.typeString(jarClass, jarClass.isEnum()));
        this.genericS = Expressions.fromString(jarClass.compileTypeParameters());
        this.nameS = Expressions.fromString(jarClass.name());
        this.extendsS = Expressions.fromString(jarClass.compileHeaderExtends());
        this.implementsS = Expressions.of(jarClass.compileHeaderImplements());
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

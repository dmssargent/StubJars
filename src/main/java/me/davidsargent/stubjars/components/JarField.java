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

package me.davidsargent.stubjars.components;

import me.davidsargent.stubjars.components.writer.JavaClassWriter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import static me.davidsargent.stubjars.components.writer.Constants.EMPTY_STRING;


public class JarField extends JarModifers implements CompileableString {
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

    public Class<?> returnType() {
        return field.getType();
    }

    public Type genericReturnType() {
        return field.getGenericType();
    }

    @Override
    public String compileToString() {
        // Figure method signature
        final String security = security().getModifier() + (security() == SecurityModifier.PACKAGE ? EMPTY_STRING : " ");
        final String finalS = isFinal() ? "final " : EMPTY_STRING;
        final String staticS = isStatic() ? "static " : EMPTY_STRING;
        final String volatileS = isVolatile() ? "volatile " : EMPTY_STRING;
        final String transientS = isTransient() ? "transient " : EMPTY_STRING;
        final String returnTypeS = JarType.toString(genericReturnType());
        final String nameS = name();

        final String assignmentS;
        if (isFinal()) {
            assignmentS = String.format(" = %s", JavaClassWriter.defaultValueForType(genericReturnType()));
        } else {
            assignmentS = EMPTY_STRING;
        }

        return String.format("%s%s%s%s%s%s %s%s;", security, finalS, staticS, volatileS, transientS, returnTypeS, nameS, assignmentS);
    }

    public JarClass<?> getClazz() {
        return jarClass;
    }

    public boolean isSynthetic() {
        return field.isSynthetic();
    }
}

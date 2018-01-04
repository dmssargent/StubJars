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

import java.lang.reflect.Modifier;

public abstract class JarModifers {
    protected abstract int getModifiers();
    public SecurityModifier security() {
        int modifiers = getModifiers();
        if (Modifier.isPrivate(modifiers)) {
            return SecurityModifier.PRIVATE;
        } else if (Modifier.isProtected(modifiers)) {
            return SecurityModifier.PROTECTED;
        } else if (Modifier.isPublic(modifiers)) {
            return SecurityModifier.PUBLIC;
        } else {
            return SecurityModifier.PACKAGE;
        }
    }

    public boolean isFinal() {
        return Modifier.isFinal(getModifiers());
    }

    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }
}

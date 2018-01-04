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

package davidsar.gent.stubjars.components.expressions;

import davidsar.gent.stubjars.components.SecurityModifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SecurityModifierExpression extends Expression {
    private final SecurityModifier modifier;

    public SecurityModifierExpression(SecurityModifier modifier) {
        this.modifier = modifier;
    }

    @Override
    public boolean hasChildren() {
        return modifier != SecurityModifier.PACKAGE;
    }

    @Override
    public List<Expression> children() {
        if (modifier == SecurityModifier.PACKAGE) {
            return Collections.emptyList();
        }

        return Arrays.asList(new StringExpression(modifier.getModifier()), StringExpression.SPACE);
    }
}

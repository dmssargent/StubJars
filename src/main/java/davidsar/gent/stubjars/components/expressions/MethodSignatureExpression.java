package davidsar.gent.stubjars.components.expressions;

import davidsar.gent.stubjars.Utils;

import java.util.Arrays;
import java.util.List;

public class MethodSignatureExpression extends Expression implements FormattedExpression {
    private Expression name;
    private Expression parameters;

    public MethodSignatureExpression(Expression name, Expression[] parameters) {
        this.name = name;
        this.parameters = Utils.arrayToListExpression(parameters);
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public List<Expression> children() {
        return Arrays.asList(name, parameters);
    }

    @Override
    public Expression getFormattedString() {
        return Expressions.fromString(name.toString(), "(", parameters.toString(), ")");
    }
}

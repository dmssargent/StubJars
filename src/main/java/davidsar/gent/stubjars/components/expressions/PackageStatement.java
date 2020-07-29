package davidsar.gent.stubjars.components.expressions;

import davidsar.gent.stubjars.components.writer.Constants;

public class PackageStatement extends StatementExpression implements FormattedExpression {
    private final String packageName;

    public PackageStatement(String packageName) {
        super(Expressions.of(StringExpression.PACKAGE, StringExpression.SPACE, Expressions.fromString(packageName)));
        this.packageName = packageName;
    }

    @Override
    public Expression getFormattedString() {
        return Expressions.fromString(Constants.PACKAGE, Constants.SPACE, packageName, Constants.SEMICOLON);
    }
}

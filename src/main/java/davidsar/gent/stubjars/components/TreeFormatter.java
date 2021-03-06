package davidsar.gent.stubjars.components;

import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.FormattedExpression;
import davidsar.gent.stubjars.components.expressions.Line;
import davidsar.gent.stubjars.components.expressions.StringExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeFormatter {
    public static List<String> toLines(Expression expression) {
        if (!expression.hasChildren()) {
            return Collections.singletonList(new Line(0, Collections.singletonList(expression)).toString());
        }

        List<String> lines = new ArrayList<>();
        List<Expression> currentLine = new ArrayList<>();
        int currentIndentLevel = 0;
        for (Expression child : flattenToAtomicExpressions(expression)) {
            if (child.equals(StringExpression.SPACE) ||
                child.equals(StringExpression.NEW_LINE) ||
                child.equals(StringExpression.EMPTY) ||
                child.equals(StringExpression.INDENT)) {
                continue;
            }
            currentLine.add(child);
            if (child.equals(StringExpression.RIGHT_CURLY)) {
                currentIndentLevel--;
            }
            if (child.equals(StringExpression.SEMICOLON) ||
                child.equals(StringExpression.LEFT_CURLY) ||
                child.equals(StringExpression.RIGHT_CURLY)) {
                lines.add(new Line(currentIndentLevel, currentLine).toString());
                currentLine = new ArrayList<>();
            }
            if (child.equals(StringExpression.LEFT_CURLY)) {
                currentIndentLevel++;
            }
        }
        return lines;
    }

    private static List<Expression> flattenToAtomicExpressions(Expression expression) {
        if (expression instanceof FormattedExpression) {
            return Collections.singletonList(((FormattedExpression) expression).getFormattedString());
        }

        if (!expression.hasChildren()) {
            return Collections.singletonList(expression);
        }

        List<Expression> result = new ArrayList<>();
        for (Expression child : expression.children()) {
            result.addAll(flattenToAtomicExpressions(child));
        }
        return result;
    }
}

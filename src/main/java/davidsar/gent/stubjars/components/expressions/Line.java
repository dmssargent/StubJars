package davidsar.gent.stubjars.components.expressions;

import davidsar.gent.stubjars.components.writer.Constants;

import java.util.List;

public class Line {
    private final List<Expression> expressions;
    private int indentLevel;

    public Line(int indentLevel, List<Expression> expressions) {
        this.expressions = expressions;
        this.indentLevel = indentLevel;
    }

    @Override
    public String toString() {
        return buildIndent() + buildLineContents();
    }

    private String buildIndent() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append(Constants.INDENT);
        }
        return stringBuilder.toString();
    }

    private String buildLineContents() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < expressions.size(); i++) {
            result.append(expressions.get(i).toString());
            if (i < expressions.size() - 1) {
                if (!expressions.get(i + 1).equals(StringExpression.SEMICOLON)) {
                    result.append(Constants.SPACE);
                }
            }
        }
        return result.toString();
    }
}

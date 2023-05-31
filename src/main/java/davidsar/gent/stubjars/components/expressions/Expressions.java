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

import davidsar.gent.stubjars.components.writer.Constants;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Expressions {
    static StatementExpression toStatement(Expression expression) {
        return new StatementExpression(expression);
    }

    @NotNull
    public static IndentedExpression<Expression> indent(Expression... expressions) {
        return new IndentedExpression<>(of(expressions));
    }

    @NotNull
    public static Expression indent(Expression expression, int times) {
        for (int i = 0; i < times; i++) {
            expression = new IndentedExpression<>(expression);
        }

        return expression;
    }

    @NotNull
    public static BlockStatement emptyBlock() {
        return new BlockStatement();
    }

    @NotNull
    public static BlockStatement blockWith(Expression... statements) {
        return new BlockStatement(statements);
    }

    public static Parenthetical asParenthetical(Expression inner) {
        return new Parenthetical(inner);
    }

    @NotNull
    public static Expression toCast(String type, String value) {
        return toCast(fromString(type), fromString(value));
    }

    @NotNull
    public static Expression toCast(Expression type, Expression value) {
        return Expressions.of(asParenthetical(type), StringExpression.SPACE, value);
    }

    @NotNull
    public static Expression toCast(Class<?> type, Object value) {
        return toCast(type.getSimpleName(), value.toString());
    }

    @NotNull
    public static MethodCall toMethodCall(String methodName) {
        return toMethodCall(methodName, StringExpression.EMPTY);
    }

    @NotNull
    public static MethodCall toMethodCall(String methodName, Expression params) {
        return new MethodCall(new StringExpression(methodName), asParenthetical(params));
    }

    @NotNull
    static Expression toSpaceAfter(@NotNull Expression expression) {
        if (!expression.hasChildren() && expression.toString().equals(Constants.EMPTY_STRING)) {
            return StringExpression.EMPTY;
        }

        return of(expression, StringExpression.SPACE);
    }

    @NotNull
    public static Expression toSpaceAfter(@NotNull String string) {
        if (string.isEmpty()) {
            return StringExpression.EMPTY;
        }

        return toSpaceAfter(fromString(string));
    }

    public static TypeExpression forType(Type type, Expression typeString) {
        return new TypeExpression(type, typeString);
    }

    public static ListExpression makeListFrom(Expression... expressions) {
        return new ListExpression(expressions, ListExpression.DELIMITER_COMMA_SPACE);
    }

    public static ListExpression makeListFrom(Stream<Expression> expressionStream) {
        Expression[] expressions = expressionStream.toArray(Expression[]::new);
        try {
            return new ListExpression(expressions, ListExpression.DELIMITER_COMMA_SPACE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to make list from expression: " + Arrays.toString(expressions), e);
        }
    }

    public static Expression when(boolean condition, Expression expression) {
        if (condition) {
            return expression;
        }

        return StringExpression.EMPTY;
    }

    public static Expression whenWithSpace(boolean condition, Expression string) {
        if (condition) {
            return toSpaceAfter(string);
        }

        return StringExpression.EMPTY;
    }

    public static StatementExpression stringAsStatement(String statement) {
        return new StatementExpression(statement);
    }

    static String stringOf(Expression... expressions) {
        return Stream.of(expressions).map(Expression::toString).collect(Collectors.joining());
    }

    public static Expression of(Expression... expressions) {
        return new GenericExpression(expressions);
    }

    public static StringExpression fromString(String string) {
        return new StringExpression(string);
    }

    public static Expression fromString(String... elements) {
        StringBuilder result = new StringBuilder();
        for (String element : elements) {
            result.append(element);
        }
        return Expressions.fromString(result.toString());
    }

    public static Expression[] flatten(Collection<Expression> children) {
        return flatten(children.stream());
    }


    private static Expression[] flatten(@NotNull Stream<Expression> expressionStream) {
        return expressionStream.flatMap(expression -> {
            if (expression == null) {
                return Stream.empty();
            }

            if (expression.hasChildren()) {
                return Stream.of(flatten(expression.children()));
            }

            return Stream.of(expression);
        }).toArray(Expression[]::new);
    }
}

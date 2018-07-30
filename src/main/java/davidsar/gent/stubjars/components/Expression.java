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

package davidsar.gent.stubjars.components;

import davidsar.gent.stubjars.components.writer.Constants;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Expression {
    StatementExpression statement() {
        return statement(this);
    }

    private static StatementExpression statement(Expression expression) {
        return new StatementExpression(expression);
    }

    @NotNull
    static BlockStatement block() {
        return new BlockStatement();
    }

    @NotNull
    static BlockStatement block(StatementExpression... statements) {
        return new BlockStatement(statements);
    }

    @NotNull
    static BlockStatement block(String... statements) {
        return new BlockStatement(Arrays.stream(statements).map(Expression::stringAsStatement).toArray(StatementExpression[]::new));
    }

    static Parenthetical parenthetical(String inner) {
        return new Parenthetical(new StringExpression(inner));
    }

    static Parenthetical parenthetical(Expression inner) {
        return new Parenthetical(inner);
    }

    @NotNull
    static String cast(String type, String value) {
        return cast(of(type), of(value));
    }

    @NotNull
    private static String cast(StringExpression type, StringExpression value) {
        return parenthetical(type) + Constants.SPACE + value;
    }

    @NotNull
    static String cast(Class<?> type, Object value) {
        return cast(type.getSimpleName(), value.toString());
    }

    @NotNull
    static MethodCall methodCall(String methodName) {
        return methodCall(methodName, Constants.EMPTY_STRING);
    }

    @NotNull
    static MethodCall methodCall(String methodName, String params) {
        return new MethodCall(new StringExpression(methodName), parenthetical(params));
    }

    Expression spaceAfter() {
        return spaceAfter(this);
    }

    @NotNull
    private static Expression spaceAfter(@NotNull Expression expression) {
        if (!expression.hasChildren() && expression.toString().equals(Constants.EMPTY_STRING)) {
            return StringExpression.EMPTY;
        }

        return of(expression, StringExpression.SPACE);
    }

    @NotNull
    static Expression spaceAfter(@NotNull String string) {
        if (string.isEmpty()) return StringExpression.EMPTY;

        return spaceAfter(of(string));
    }

    @NotNull
    static TypeExpression forType(Type type, String typeString) {
        return new TypeExpression(type, typeString);
    }

    static Expression when(boolean condition, Expression expression) {
        if (condition) return expression;

        return StringExpression.EMPTY;
    }

    @NotNull
    static Expression whenWithSpace(boolean condition, String string) {
        if (condition) return spaceAfter(string);

        return StringExpression.EMPTY;
    }

    private static StatementExpression stringAsStatement(String statement) {
        return new StatementExpression(statement);
    }

    private static String stringOf(Expression... expressions) {
        return Stream.of(expressions).map(Expression::toString).collect(Collectors.joining());
    }

    static Expression of(Expression... expressions) {
        return new GenericExpression(expressions);
    }

    private static Expression[] flatten(Collection<Expression> children) {
        return flatten(children.stream());
    }

    private static Expression[] flatten(@NotNull Stream<Expression> expressionStream) {
        return expressionStream.flatMap(expression -> {
            if (expression.hasChildren()) {
                return Stream.of(flatten(expression.children()));
            }

            return Stream.of(expression);
        }).toArray(Expression[]::new);
    }

    static StringExpression of(String string) {
        return new StringExpression(string);
    }

    protected abstract boolean hasChildren();

    protected abstract List<Expression> children();

    @Override
    public String toString() {
        return stringOf(flatten(children()));
    }

    public final static class StringExpression extends Expression {
        static final Expression LPAREN = new StringExpression(Constants.LEFT_PAREN);
        static final Expression RPAREN = new StringExpression(Constants.RIGHT_PAREN);
        static final Expression SPACE = new StringExpression(Constants.SPACE);
        static final Expression COMMA = new StringExpression(Constants.COMMA);
        static final Expression INDENT = new StringExpression(Constants.INDENT);
        static final Expression PERIOD = new StringExpression(Constants.PERIOD);
        static final Expression AT = new StatementExpression(Constants.AT);
        private static final Expression LCURLY = new StringExpression(Constants.LCURLY);
        private static final Expression RCURLY = new StringExpression(Constants.RCURLY);
        static final StringExpression EMPTY = new StringExpression(Constants.EMPTY_STRING);
        static final StringExpression SEMICOLON = new StringExpression(Constants.SEMICOLON);
        static final StringExpression NEW_LINE = new StringExpression(Constants.NEW_LINE_CHARACTER);
        private final String data;
        private static final Expression[] EMPTY_ARRAY = new Expression[0];

        private StringExpression(String data) {
            this.data = data;
        }

        @Override
        public boolean hasChildren() {
            return false;
        }

        @Override
        public List<Expression> children() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return data;
        }
    }

    public static class StatementExpression extends Expression {
        private final Expression expression;
        private final boolean implString;
        private List<Expression> children;

        private StatementExpression(Expression expression) {
            this.expression = expression;
            implString = false;
        }

        private StatementExpression(String statement) {
            expression = new StringExpression(statement);
            implString = true;
        }

        @Override
        public boolean hasChildren() {
            return true;
        }

        @Override
        public List<Expression> children() {
            if (children == null) {
                children = Collections.unmodifiableList(buildChildrenList());
            }

            return children;
        }

        @NotNull
        private List<Expression> buildChildrenList() {
            if (implString) {
                return Collections.singletonList(expression);
            }

            return Arrays.asList(expression, StringExpression.SEMICOLON, StringExpression.NEW_LINE);
        }
    }

    public final static class BlockStatement extends Expression {
        private final StatementExpression[] statements;
        private List<Expression> children;

        private BlockStatement(StatementExpression... statements) {
            this.statements = statements;
        }

        @Contract(pure = true)
        @Override
        public boolean hasChildren() {
            return true;
        }

        @Override
        public List<Expression> children() {
            if (children == null) {
                children = Collections.unmodifiableList(buildChildren());
            }

            return children;
        }

        @NotNull
        private List<Expression> buildChildren() {
            if (statements.length == 0) {
                return Arrays.asList(StringExpression.LCURLY, StringExpression.RCURLY);
            }

            Expression indentedStatements = of(Arrays.stream(statements)
                    .map(statement -> of(StringExpression.INDENT, statement))
                    .toArray(Expression[]::new)
            );
            return Collections.unmodifiableList(Arrays.asList(StringExpression.LCURLY, StringExpression.NEW_LINE,
                    indentedStatements,
                    StringExpression.RCURLY, StringExpression.NEW_LINE
            ));

        }
    }

    public final static class MethodCall extends StatementExpression {
        private MethodCall(StringExpression method, Parenthetical expression) {
            super(Expression.of(method, expression));
        }
    }

    private static class Parenthetical extends Expression {
        private final Expression innerExpression;

        private Parenthetical(Expression innerExpression) {
            this.innerExpression = innerExpression;
        }

        @Override
        public boolean hasChildren() {
            return true;
        }

        @Override
        public List<Expression> children() {
            return Arrays.asList(StringExpression.LPAREN, innerExpression, StringExpression.RPAREN);
        }
    }

    public static class TypeExpression extends Expression {
        private final Expression expression;
        private final Type type;

        private TypeExpression(Type type, String expression) {
            this.type = type;
            this.expression = Expression.of(expression);
        }

        @Override
        public boolean hasChildren() {
            return true;
        }

        @Override
        public List<Expression> children() {
            return Collections.singletonList(expression);
        }
    }

    private final static class GenericExpression extends Expression {
        private final Expression[] children;

        private GenericExpression(Expression[] children) {
            this.children = children;
        }

        @Override
        public boolean hasChildren() {
            return true;
        }

        @Override
        public List<Expression> children() {
            return Arrays.asList(children);
        }
    }
}

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

import java.util.Collections;
import java.util.List;

public final class StringExpression extends Expression {
    public static final Expression GREATER_THAN = new StringExpression(Constants.GREATER_THAN);
    public static final Expression LESS_THAN = new StringExpression(Constants.LESS_THAN);
    public static final Expression QUESTION_MARK = new StringExpression(Constants.QUESTION_MARK);
    public static final Expression LEFT_BRACE = new StringExpression(Constants.LEFT_BRACE);
    public static final Expression RIGHT_BRACE = new StringExpression(Constants.RIGHT_BRACE);
    public static final Expression EXTENDS = new StringExpression(Constants.EXTENDS);
    public static final Expression SUPER = new StringExpression(Constants.SUPER);
    static final Expression LEFT_PAREN = new StringExpression(Constants.LEFT_PAREN);
    static final Expression RIGHT_PAREN = new StringExpression(Constants.RIGHT_PAREN);
    public static final Expression SPACE = new StringExpression(Constants.SPACE);
    static final Expression COMMA = new StringExpression(Constants.COMMA);
    public static final Expression INDENT = new StringExpression(Constants.INDENT);
    public static final Expression PERIOD = new StringExpression(Constants.PERIOD);
    public static final Expression AT = new StatementExpression(Constants.AT);
    public static final Expression LEFT_CURLY = new StringExpression(Constants.LEFT_CURLY);
    public static final Expression RIGHT_CURLY = new StringExpression(Constants.RIGHT_CURLY);
    public static final StringExpression EMPTY = new StringExpression(Constants.EMPTY_STRING);
    public static final StringExpression SEMICOLON = new StringExpression(Constants.SEMICOLON);
    public static final StringExpression NEW_LINE = new StringExpression(Constants.NEW_LINE_CHARACTER);
    private final String data;

    StringExpression(String data) {
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StringExpression)) {
            return false;
        }
        StringExpression expression = (StringExpression) obj;
        return data.equals(expression.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}

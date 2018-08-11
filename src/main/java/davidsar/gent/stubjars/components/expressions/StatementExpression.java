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

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StatementExpression extends Expression {
    private final Expression expression;
    private final boolean implString;
    private List<Expression> children;

    StatementExpression(Expression expression) {
        this.expression = expression;
        implString = false;
    }

    StatementExpression(String statement) {
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

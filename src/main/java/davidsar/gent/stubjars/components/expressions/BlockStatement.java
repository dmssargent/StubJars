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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BlockStatement extends Expression {
    private final Expression[] statements;
    private List<Expression> children;

    BlockStatement(Expression... statements) {
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
            return Arrays.asList(StringExpression.LEFT_CURLY, StringExpression.RIGHT_CURLY);
        }

        Expression indentedStatements = Expressions.of(Arrays.stream(statements)
            .map(Expression::indent)
            .toArray(Expression[]::new)
        );

        return Collections.unmodifiableList(Arrays.asList(StringExpression.LEFT_CURLY, StringExpression.NEW_LINE,
            indentedStatements,
            StringExpression.RIGHT_CURLY, StringExpression.NEW_LINE
        ));
    }
}

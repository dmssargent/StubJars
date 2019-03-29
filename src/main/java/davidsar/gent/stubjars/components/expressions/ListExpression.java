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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class ListExpression extends Expression implements FormattedExpression {
    static final List<Expression> DELIMITER_COMMA_NEW_LINE =
        Collections.unmodifiableList(Arrays.asList(
            StringExpression.COMMA, StringExpression.NEW_LINE
        ));

    static final List<Expression> DELIMITER_COMMA_SPACE =
        Collections.unmodifiableList(Arrays.asList(
            StringExpression.COMMA, StringExpression.SPACE
        ));

    private final Expression[] expressions;
    private final List<Expression> delimiters;
    private List<Expression> children;

    ListExpression(Expression[] expressions, List<Expression> delimiters) {
        this.expressions = expressions;
        this.delimiters = delimiters;
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public List<Expression> children() {
        if (children == null) {
            children = buildChildrenList();
        }

        return children;
    }

    @NotNull
    private List<Expression> buildChildrenList() {
        if (expressions.length == 0) {
            return Collections.emptyList();
        } else if (expressions.length == 1) {
            return Collections.singletonList(expressions[0]);
        }

        List<Expression> expressionChildren =
            new ArrayList<>(expressions.length * (delimiters.size() + 1) - delimiters.size());
        IntStream.range(0, expressions.length).forEachOrdered(index -> {
            expressionChildren.add(expressions[index]);
            if (index < expressions.length - 1) {
                expressionChildren.addAll(delimiters);
            }
        });

        return expressionChildren;
    }

    @Override
    public Expression getFormattedString() {
        return Expressions.fromString(this.toString());
    }
}

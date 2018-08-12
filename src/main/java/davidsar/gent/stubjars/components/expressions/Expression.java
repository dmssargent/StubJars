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
import java.util.List;

public abstract class Expression {
    public StatementExpression asStatement() {
        return Expressions.toStatement(this);
    }

    @NotNull
    public static BlockStatement blockWith(String... statements) {
        return new BlockStatement(Arrays.stream(statements).map(Expressions::stringAsStatement).toArray(Expression[]::new));
    }

    @NotNull
    public IndentedExpression indent() {
        return new IndentedExpression<>(this);
    }

    @NotNull
    public Parenthetical parenthetical() {
        return new Parenthetical(this);
    }

    public Expression asSpaceAfter() {
        return Expressions.toSpaceAfter(this);
    }

    protected abstract boolean hasChildren();

    public abstract List<Expression> children();

    @Override
    public String toString() {
        return Expressions.stringOf(Expressions.flatten(children()));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Expression)) {
            return false;
        }

        Expression rhs = (Expression) obj;

        if (hasChildren() != rhs.hasChildren()) {
            return false;
        }

        return children().equals(rhs.children());
    }

    @Override
    public int hashCode() {
        if (!hasChildren()) {
            throw new UnsupportedOperationException();
        }

        return children().hashCode();
    }

    public Expression asBlock() {
        return Expressions.blockWith(this);
    }
}

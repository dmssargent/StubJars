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

import static davidsar.gent.stubjars.components.expressions.Expressions.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import davidsar.gent.stubjars.components.expressions.StatementExpression;
import davidsar.gent.stubjars.components.expressions.StringExpression;
import org.junit.Test;

import java.util.Arrays;

public class ExpressionTests {
    @Test
    public void statement() {
        Expression cat = Expressions.fromString("cat");

        final StatementExpression catStatement = cat.asStatement();

        assertThat(catStatement.children(),
            is(Arrays.asList(cat, StringExpression.SEMICOLON, StringExpression.NEW_LINE)));
        assertThat(catStatement.toString(), is("cat;\n"));
    }

    @Test
    public void block_Empty() {
        final Expression expression = Expressions.emptyBlock();

        assertThat(expression.children(),
            is(Arrays.asList(StringExpression.LEFT_CURLY, StringExpression.RIGHT_CURLY)));
        assertThat(expression.toString(), is("{}"));
    }

    @Test
    public void block_SingleStatement_String() {
        final String statement = "dog();";

        Expression block = Expression.blockWith(statement);

        assertThat(block.children(),
            is(Arrays.asList(StringExpression.LEFT_CURLY, StringExpression.NEW_LINE,
                of(of(StringExpression.INDENT, Expressions.stringAsStatement(statement))),
                StringExpression.RIGHT_CURLY, StringExpression.NEW_LINE)));
    }

    @Test
    public void block_MultipleStatements() {

    }

    @Test
    public void block1() {
    }

    @Test
    public void block2() {
    }

    @Test
    public void parenthetical() {
    }

    @Test
    public void parenthetical1() {
    }

    @Test
    public void cast() {
    }

    @Test
    public void cast1() {
    }

    @Test
    public void methodCall() {
    }

    @Test
    public void methodCall1() {
    }

    @Test
    public void spaceAfter() {
    }

    @Test
    public void spaceAfter1() {
    }
}
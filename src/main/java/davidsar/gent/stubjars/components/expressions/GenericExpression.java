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

import java.util.Arrays;
import java.util.List;

final class GenericExpression extends Expression {
    private final Expression[] children;

    GenericExpression(Expression[] children) {
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

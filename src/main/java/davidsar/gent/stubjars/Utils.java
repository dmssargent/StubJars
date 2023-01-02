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

package davidsar.gent.stubjars;

import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.function.Function;

public class Utils {
    private static Logger log = LoggerFactory.getLogger(Utils.class);

    @NotNull
    public static Expression arrayToListExpression(Expression[] expressions) {
        return arrayToListExpression(expressions, x -> x);
    }

    @NotNull
    public static <T> Expression arrayToListExpression(T[] elements, Function<T, Expression> function) {
        return Expressions.makeListFrom(
            Arrays.stream(elements).map(function).toArray(Expression[]::new)
        );
    }

    @NotNull
    public static File getJavaHomeBinFromEnvironment() {
        String javaHomeString = System.getenv("JAVA_HOME");
        if (javaHomeString == null) {
            log.error("JAVA_HOME does not exist");
            throw new IllegalStateException("JAVA_HOME required but not specified");
        }
        File javaHome = new File(javaHomeString);
        if (!javaHome.isDirectory()) {
            throw new IllegalStateException("JAVA_HOME does not point a directory");
        }
        File javaHomeBin = new File(javaHome, "bin");
        if (!javaHomeBin.isDirectory()) {
            throw new IllegalStateException("JAVA_HOME points to an invalid JDK location");
        }

        return javaHomeBin;
    }
}

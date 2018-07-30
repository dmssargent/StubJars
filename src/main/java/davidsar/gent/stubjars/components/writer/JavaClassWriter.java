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

package davidsar.gent.stubjars.components.writer;

import davidsar.gent.stubjars.Preconditions;
import davidsar.gent.stubjars.components.Expression;
import davidsar.gent.stubjars.components.JarClass;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class JavaClassWriter extends Writer {
    private final JarClass<?> klazz;
    private String compiledString;

    public JavaClassWriter(@NotNull final File file, @NotNull final JarClass<?> klazz, @NotNull WriterThread writerThread) {
        super(file, writerThread);
        Preconditions.checkNotNull(file);
        Preconditions.checkNotNull(klazz);
        this.klazz = klazz;
    }

    @NotNull
    private String compile() {
        if (compiledString == null) {
            this.compiledString = compile(klazz)
                    .replaceAll("(\\s*\\n )+\\s*\\n(\\s*)", "\n\n$2")
                    .replaceAll("([^ \\t\\n])[\\t ]+([^ \\t])", "$1 $2");
        }

        return compiledString;
    }

    @NotNull
    private static String compile(@NotNull final JarClass<?> klazz) {
        String packageStatement = compilePackageStatement(klazz);
        Expression classBody = compileClass(klazz);
        return String.format("%s\n%s", packageStatement, classBody);
    }

    @NotNull
    private static Expression compileClass(@NotNull final JarClass<?> klazz) {
        return klazz.compileToExpression();
    }

    /**
     * Produces a String containing a source code version of the package name declaration
     *
     * @param klazz the {@link JarClass} to create the declaration for
     * @return source code version of the package name declaration
     */
    @NotNull
    private static String compilePackageStatement(@NotNull final JarClass<?> klazz) {
        return String.format("package %s;\n", klazz.packageName());
    }

    public void write() {
        writeDataWithDedicatedThread(compile());
    }
}

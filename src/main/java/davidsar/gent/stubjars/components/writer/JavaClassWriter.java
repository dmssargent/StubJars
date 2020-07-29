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

import davidsar.gent.stubjars.components.JarClass;
import davidsar.gent.stubjars.components.TreeFormatter;
import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import davidsar.gent.stubjars.components.expressions.PackageStatement;
import davidsar.gent.stubjars.components.expressions.StringExpression;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

public class JavaClassWriter extends Writer {
    private final JarClass<?> klazz;
    private String compiledString;

    /**
     * Builds a new {@link JavaClassWriter} for the given file associated with the given
     * {@link JarClass}. The resulting {@code JavaClassWriter} will be bound to the given
     * {@link WriterThread}.
     *
     * @param file         the file to write to
     * @param clazz        the representative class
     * @param writerThread the thread to use for Writing
     */
    public JavaClassWriter(@NotNull final File file, @NotNull final JarClass<?> clazz,
                           @NotNull WriterThread writerThread) {
        super(
            Objects.requireNonNull(file, "file is null"),
            Objects.requireNonNull(writerThread, "writer thread is null")
        );
        this.klazz = Objects.requireNonNull(clazz, "class is null");
    }

    @NotNull
    private String compile() {
        if (compiledString == null) {
            this.compiledString = String.join(Constants.NEW_LINE_CHARACTER, TreeFormatter.toLines(compile(klazz)));
        }

        return compiledString;
    }

    @NotNull
    private static Expression compile(@NotNull final JarClass<?> klazz) {
        Expression packageStatement = compilePackageStatement(klazz);
        Expression classBody = compileClass(klazz);
        return Expressions.of(packageStatement, StringExpression.NEW_LINE, classBody);
    }

    @NotNull
    private static Expression compileClass(@NotNull final JarClass<?> klazz) {
        return klazz.compileToExpression();
    }

    /**
     * Produces a String containing a source code version of the package name declaration.
     *
     * @param clazz the {@link JarClass} to create the declaration for
     * @return source code version of the package name declaration
     */
    @NotNull
    private static Expression compilePackageStatement(@NotNull final JarClass<?> clazz) {
        return new PackageStatement(clazz.packageName());
    }

    public void write() {
        writeDataWithDedicatedThread(compile());
    }
}

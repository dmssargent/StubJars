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

import davidsar.gent.stubjars.components.JarClass;
import davidsar.gent.stubjars.components.SecurityModifier;
import davidsar.gent.stubjars.components.writer.JavaClassWriter;
import davidsar.gent.stubjars.components.writer.Writer;
import davidsar.gent.stubjars.components.writer.WriterThread;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * The main class for StubJars.
 *
 * @see #builder() to create an instance of {@link StubJars}
 */
public class StubJars {
    private static final Logger log = LoggerFactory.getLogger(StubJars.class);
    private final ConcurrentMap<Class<?>, JarClass<?>> clazzes;
    private List<Package> packages;
    private static final File SOURCE_DIR = new File("stub_src");
    private static final File BUILD_DIR = new File(SOURCE_DIR, "build");
    private static final File CLASSES_DIR = new File(BUILD_DIR, "classes");
    private static final File SOURCES_LIST_FILE = new File(SOURCE_DIR, "sources.list");


    private StubJars(@NotNull ConcurrentMap<Class<?>, JarClass<?>> clazzes) {
        this.clazzes = clazzes;
    }

    /**
     * Returns a new StubJars builder to create a new StubJars instance.
     *
     * @return a new StubJars builder
     */
    @NotNull
    static Builder builder() {
        return new Builder();
    }

    void createDirectoryTree() {
        if (packages == null) {
            buildPackagesList();
        }
        SOURCE_DIR.mkdirs();
        createBuildDir();
        for (Package e : packages) {
            File eFile = new File(SOURCE_DIR, e.getName().replace('.', File.separatorChar));
            eFile.mkdirs();
        }
    }

    private void buildPackagesList() {
        packages = new ArrayList<>();
        for (Class clazz : clazzes.keySet()) {
            if (!packages.contains(clazz.getPackage())) {
                packages.add(clazz.getPackage());
            }
        }
    }

    void createSourceFiles() {
        WriterThread writerThread = new WriterThread();
        writerThread.start();
        StringBuilder sourceFiles = new StringBuilder();
        for (JarClass e : clazzes.values()) {
            if (e.isInnerClass() || e.name().isEmpty() || e.security() == SecurityModifier.PRIVATE) {
                continue;
            }

            // this breaks compilation (currently)
            // todo: make unneeded
            if (e.getClazz().getName().equals(Enum.class.getName())) {
                continue;
            }

            File file = new File(SOURCE_DIR, e.getClazz().getName()
                .replace('.', File.separatorChar) + ".java");
            JavaClassWriter writer = new JavaClassWriter(file, e, writerThread);
            sourceFiles.append(file.getAbsolutePath()).append(System.lineSeparator());
            writer.write();
        }

        writerThread.done();
        try {
            writerThread.waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        Writer sourcesList = new Writer(SOURCES_LIST_FILE);
        try {
            sourcesList.write(sourceFiles.toString());
            writerThread.join();
        } catch (Exception e) {
            log.error("Failed to write files", e);
        }
    }

    private void createBuildDir() {
        BUILD_DIR.mkdirs();
        CLASSES_DIR.mkdirs();
    }

    @NotNull File getSourceDestination() {
        return SOURCE_DIR;
    }

    /**
     * Creates new {@link StubJars} instances.
     */
    static class Builder {
        private final List<JarFile> jars;
        private final List<JarFile> classpathJars;

        private Builder() {
            jars = new ArrayList<>();
            classpathJars = new ArrayList<>();
        }

        /**
         * Add a JAR file for {@link StubJars} to manage.
         *
         * @param jar a {@link File} representing a JAR file
         */
        void addJar(@NotNull File jar) {
            jars.add(JarFile.forFile(jar));
        }

        /**
         * Add a JAR file for {@link StubJars}, that provides classpath info.
         *
         * @param jar a {@link File} representing a JAR file
         */
        void addClasspathJar(@NotNull File jar) {
            classpathJars.add(JarFile.forFile(jar));
        }

        /**
         * Adds JAR files for {@link StubJars} to manage.
         *
         * @param jars the {@link File}s representing a JAR files
         */
        void addJars(@NotNull File... jars) {
            for (File jar : jars) {
                addJar(jar);
            }
        }

        /**
         * Creates the actual {@link StubJars} instance. This method may take some time to execute.
         *
         * @return a new {@link StubJars} instance
         */
        @NotNull StubJars build() {
            ClassLoader cpClassLoader = JarFile.createClassLoaderFromJars(null, classpathJars.toArray(new JarFile[0]));
            ClassLoader classLoader = JarFile.createClassLoaderFromJars(cpClassLoader, jars.toArray(new JarFile[0]));
            ConcurrentMap<Class<?>, JarClass<?>> klazzes = new ConcurrentHashMap<>();
            for (JarFile jar : jars) {
                final Set<JarClass<?>> classes;
                try {
                    classes = jar.getClasses(classLoader);
                } catch (IOException e) {
                    throw new RuntimeException("Cannot load jar!", e);
                }

                ConcurrentMap<Class<?>, JarClass<?>> klazzesFromJar = classes.stream()
                    .collect(Collectors.toConcurrentMap(JarClass::getClazz, a -> a));
                klazzes.putAll(klazzesFromJar);
            }

            JarClass.loadClassToJarClassMap(klazzes);
            return new StubJars(klazzes);
        }
    }
}

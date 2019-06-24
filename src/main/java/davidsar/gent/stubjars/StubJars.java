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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The main class for StubJars.
 *
 * @see #builder() to create an instance of {@link StubJars}
 */
public class StubJars {
    private static final Logger log = LoggerFactory.getLogger(StubJars.class);
    private final List<JarClass<?>> clazzes;
    private final List<JarFile> classpathJars;
    private final ClassLoader stubClassLoader;
    private List<Package> packages;
    private static final File SOURCE_DIR = new File("stub_src");
    private static final File BUILD_DIR = new File(SOURCE_DIR, "build");
    private static final File CLASSES_DIR = new File(BUILD_DIR, "classes");
    private static final File SOURCES_LIST_FILE = new File(SOURCE_DIR, "sources.list");
    private final int numberOfCompilerThreads = 4;


    private StubJars(@NotNull List<JarClass<?>> clazzes, List<JarFile> classpathJars, ClassLoader classLoader) {
        this.clazzes = clazzes;
        this.classpathJars = classpathJars;
        this.stubClassLoader = classLoader;
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
        for (JarClass clazz : clazzes) {
            if (!packages.contains(clazz.getClazz().getPackage())) {
                packages.add(clazz.getClazz().getPackage());
            }
        }
    }

    void createSourceFiles() {
        System.setSecurityManager(new StubJarsSecurityManager());
        WriterThread writerThread = startWriterThread();
        StringBuilder sourceFiles = new StringBuilder();

        ExecutorService threads = Executors.newFixedThreadPool(numberOfCompilerThreads);
        submitCompilerJobs(writerThread, sourceFiles, threads);

        if (!waitForFinish(writerThread, threads)) {
            return;
        }

        writeSourceFileList(sourceFiles);
        waitForWriterThreadToFinish(writerThread);
    }

    @NotNull
    private WriterThread startWriterThread() {
        WriterThread writerThread = new WriterThread();
        writerThread.start();
        return writerThread;
    }

    private void waitForWriterThreadToFinish(WriterThread writerThread) {
        try {
            writerThread.join();
        } catch (Exception e) {
            log.error("Failed to write files", e);
        }
    }

    private void writeSourceFileList(StringBuilder sourceFiles) {
        Writer sourcesList = new Writer(SOURCES_LIST_FILE);
        try {
            sourcesList.write(sourceFiles.toString());
        } catch (IOException e) {
            log.error("Failed to write source file list", e);
        }
    }

    private boolean waitForFinish(WriterThread writerThread, ExecutorService threads) {
        threads.shutdown();
        try {
            threads.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
            writerThread.done();
            writerThread.waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    private void submitCompilerJobs(WriterThread writerThread, StringBuilder sourceFiles, ExecutorService threads) {
        Semaphore lock = new Semaphore(1, true);
        for (int iThread = 0; iThread < numberOfCompilerThreads; ++iThread) {
            final int segmentSize = clazzes.size() / numberOfCompilerThreads;
            List<JarClass<?>> list = Collections.unmodifiableList(
                clazzes.subList(
                    iThread * segmentSize,
                    iThread == numberOfCompilerThreads - 1
                        ? clazzes.size() : segmentSize * (iThread + 1))
            );
            threads.execute(new CompilerThread(list, writerThread, lock, sourceFiles));
        }
    }

    private void createBuildDir() {
        BUILD_DIR.mkdirs();
        CLASSES_DIR.mkdirs();
    }

    @NotNull File getSourceDestination() {
        return SOURCE_DIR;
    }

    public void compileGeneratedCode() throws IOException, InterruptedException {
        File javaHomeBin = Utils.getJavaHomeBinFromEnvironment();

        List<String> javacProcessArgs = new ArrayList<>();
        javacProcessArgs.add(new File(javaHomeBin, "javac").getPath());
        if (!classpathJars.isEmpty()) {
            javacProcessArgs.add("-cp");
            javacProcessArgs.add(
                classpathJars.stream()
                    .map(jar -> jar.getJar().getPath())
                    .collect(Collectors.joining(File.pathSeparator))
            );
        }
        javacProcessArgs.add("-d");
        javacProcessArgs.add(BUILD_DIR.getPath());
        javacProcessArgs.add(String.format("@%s", SOURCES_LIST_FILE.getPath()));

        Process javac = new ProcessBuilder(javacProcessArgs)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start();

        if (javac.waitFor() == 0) {
            return;
        }

        log.error("javac failed with error code {}", javac.exitValue());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(javac.getInputStream(), UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.error(line);
            }
        }
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
        void addClasspathJar(@NotNull File jar) throws IOException {
            if (!jar.exists()) {
                throw new IOException("A provided classpath JAR doesn't exist. File: " + jar.getAbsolutePath());
            }
            classpathJars.add(JarFile.forFile(jar));
        }

        /**
         * Adds JAR files for {@link StubJars} to manage.
         *
         * @param jars the {@link File}s representing a JAR files
         */
        void addJars(@NotNull File... jars) throws IOException {
            for (File jar : jars) {
                if (!jar.exists()) {
                    throw new IOException("A provided JAR doesn't exist. File: " + jar.getName());
                }
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
            List<JarClass<?>> clazzes = Collections.synchronizedList(new ArrayList<>());
            for (JarFile jar : jars) {
                final Set<JarClass<?>> classes;
                try {
                    classes = jar.getClasses(classLoader);
                } catch (IOException e) {
                    throw new RuntimeException("Cannot load jar!", e);
                }

                clazzes.addAll(classes);
            }

            JarClass.loadJarClassList(clazzes);
            return new StubJars(clazzes, classpathJars, classLoader);
        }
    }

    private static class CompilerThread implements Runnable {
        private final List<JarClass<?>> list;
        private final WriterThread writerThread;
        private final Semaphore lock;
        private final StringBuilder sourceFiles;

        public CompilerThread(List<JarClass<?>> list, WriterThread writerThread, Semaphore lock, StringBuilder sourceFiles) {
            this.list = list;
            this.writerThread = writerThread;
            this.lock = lock;
            this.sourceFiles = sourceFiles;
        }

        @Override
        public void run() {
            for (JarClass e : list) {
                if (e.isInnerClass()
                    || e.name().isEmpty()
                    || e.security() == SecurityModifier.PRIVATE
                    || e.fullName().equals(Enum.class.getName())) {
                    continue;
                }

                File file = new File(SOURCE_DIR, e.fullName().replace('.', File.separatorChar) + ".java");
                JavaClassWriter writer = new JavaClassWriter(file, e, writerThread);
                writer.write();
                try {
                    lock.acquire();
                } catch (InterruptedException e1) {
                    return;
                }
                sourceFiles.append(file.getAbsolutePath()).append(System.lineSeparator());
                lock.release();
            }
        }
    }
}

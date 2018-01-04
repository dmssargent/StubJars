package me.davidsargent.stubjars;

import me.davidsargent.stubjars.components.JarClass;
import me.davidsargent.stubjars.components.SecurityModifier;
import me.davidsargent.stubjars.components.writer.JavaClassWriter;
import me.davidsargent.stubjars.components.writer.Writer;
import me.davidsargent.stubjars.components.writer.WriterThread;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * The main class for StubJars
 *
 * @see #builder() to create an instance of {@link StubJars}
 */
public class StubJars {
    private final ConcurrentMap<Class<?>, JarClass<?>> klazzes;
    private List<Package> packages;
    private final File SOURCE_DIR = new File("stub_src");
    private final File BUILD_DIR = new File(SOURCE_DIR, "build");
    private final File CLASSES_DIR = new File(BUILD_DIR, "classes");
    private final File SOURCES_LIST_FILE = new File(SOURCE_DIR, "sources.list");

    private StubJars(@NotNull ConcurrentMap<Class<?>, JarClass<?>> klazzes, @NotNull ClassLoader classLoader) {
        this.klazzes = klazzes;
    }

    /**
     * Returns a new StubJars builder to create a new StubJars instance
     *
     * @return a new StubJars builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    public void createDirectoryTree() {
        if (packages == null) buildPackagesList();
        SOURCE_DIR.mkdirs();
        createBuildDir();
        for (Package e : packages) {
            File eFile = new File(SOURCE_DIR, e.getName().replace('.', File.separatorChar));
            eFile.mkdirs();
        }
    }

    private void buildPackagesList() {
        packages = new LinkedList<>();
        for (Class klazz : klazzes.keySet()) {
            if (!packages.contains(klazz.getPackage()))
                packages.add(klazz.getPackage());
        }
    }

    public void createSourceFiles() {
        WriterThread writerThread = new WriterThread();
        writerThread.start();
        StringBuilder sourceFiles = new StringBuilder();
        for (JarClass e : klazzes.values()) {
            if (e.isInnerClass() || e.name().isEmpty() || e.security() == SecurityModifier.PRIVATE)
                continue;
            // this breaks compilation (currently)
            // todo: make unneeded
            if (e.getKlazz().getName().equals("java.lang.Enum"))
                continue;
            File file = new File(SOURCE_DIR, e.getKlazz().getName().replace('.', File.separatorChar) + ".java");
            JavaClassWriter writer = new JavaClassWriter(file, e, writerThread);
            sourceFiles.append(file.getAbsolutePath()).append(System.lineSeparator());
            writer.write();
        }
        writerThread.done();
        Writer sourcesList = new Writer(SOURCES_LIST_FILE);
        try {
            sourcesList.write(sourceFiles.toString());
            writerThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createBuildDir() {
        BUILD_DIR.mkdirs();
        CLASSES_DIR.mkdirs();
    }

    @NotNull
    public File getSourceDestination() {
        return SOURCE_DIR;
    }

    /**
     * Creates new {@link StubJars} instances
     */
    public static class Builder {
        private final List<JarFile> jars;

        private Builder() {
            jars = new ArrayList<>();
        }

        /**
         * Add a JAR file for {@link StubJars} to manage
         *
         * @param jar a {@link File} representing a JAR file
         */
        public void addJar(@NotNull File jar) {
            jars.add(JarFile.forFile(jar));
        }

        /**
         * Adds JAR files for {@link StubJars} to manage
         *
         * @param jars the {@link File}s representing a JAR files
         */
        public void addJars(@NotNull File... jars) {
            for (File jar : jars) {
                addJar(jar);
            }
        }

        /**
         * Creates the actual {@link StubJars} instance. This method may take some time to execute.
         *
         * @return a new {@link StubJars} instance
         */
        @NotNull
        public StubJars build() {
            ClassLoader classLoader = JarFile.createClassLoaderFromJars(jars.toArray(new JarFile[jars.size()]));
            ConcurrentMap<Class<?>, JarClass<?>> klazzes = new ConcurrentHashMap<>();
            for (JarFile jar : jars) {
                final Set<JarClass<?>> classes;
                try {
                    classes = jar.getClasses(classLoader);
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("Cannot load jar!", e);
                }

                ConcurrentMap<Class<?>, JarClass<?>> klazzesFromJar = classes.stream()
                        .collect(Collectors.toConcurrentMap(JarClass::getKlazz, a -> a));
                klazzes.putAll(klazzesFromJar);
            }

            JarClass.loadClassToJarClassMap(klazzes);
            return new StubJars(klazzes, classLoader);
        }
    }
}

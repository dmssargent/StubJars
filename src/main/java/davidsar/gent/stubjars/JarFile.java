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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import davidsar.gent.stubjars.components.JarClass;
import davidsar.gent.stubjars.utils.Streams;

public class JarFile {
    private static final Logger log = LoggerFactory.getLogger(JarFile.class);
    private static final Map<File, JarFile> jarFiles = new HashMap<>();
    private final File jar;

    private JarFile(File jar) {
        this.jar = jar;
    }

    static JarFile forFile(@NotNull File jar) {
        if (jarFiles.containsKey(jar)) {
            return jarFiles.get(jar);
        }

        JarFile jarFile = new JarFile(jar);
        jarFiles.put(jar, jarFile);
        return jarFile;
    }

    @NotNull
    static ClassLoader createClassLoaderFromJars(@Nullable ClassLoader parentClassLoader, String... jarFiles) {
        URL[] urls = Arrays.stream(jarFiles).map(File::new).map(JarFile::forFile).map(JarFile::getUrl).toArray(URL[]::new);
        ClassLoader classLoader = parentClassLoader == null ? JarFile.class.getClassLoader() : parentClassLoader;
        return new URLClassLoader(urls, classLoader);
    }

    @NotNull
    private URL getUrl() {
        try {
            return jar.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format("Could not create classloader for \"%s\"", jar.getAbsolutePath()), e);
        }
    }

    Set<JarClass<?>> getClasses(ClassLoader loader) throws IOException {
        try (java.util.jar.JarFile iJar = new java.util.jar.JarFile(jar)) {
            return Streams.makeFor(iJar.entries())
                    .filter(jarEntry -> jarEntry.getName().endsWith(".class"))
                    .map(entry -> {
                        try {
                            return new JarClass<>(loader, entry.getName());
                        } catch (ClassNotFoundException e) {
                            log.error("unable to load class: not found: ignored: " + entry.getName());
                            return null;
                        } catch (LinkageError e) {
                            log.error("unable to load class: linkage error: ignored: " + entry.getName());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .flatMap(clazz -> Stream.concat(Stream.of(clazz), findInnerClasses(clazz)))
                    .collect(Collectors.toSet());
        }
    }

    @NotNull
    private Stream<JarClass<?>> findInnerClasses(@NotNull JarClass<?> jarClass) {
        return jarClass.innerClasses().values().stream()
            .flatMap(clazz -> {
                Stream<JarClass<?>> stream = clazz.innerClasses().values().stream();
                Stream<JarClass<?>> innerClasses = findInnerClasses(clazz);
                return Stream.concat(Stream.concat(Stream.of(clazz), stream), innerClasses);
                });
    }

    public File getJar() {
        return jar;
    }
}

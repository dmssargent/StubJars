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

package me.davidsargent.stubjars;

import me.davidsargent.stubjars.components.JarClass;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;

public class JarFile {
    private static Map<File, JarFile> jarFiles = new HashMap<>();
    private final File jar;
    private ClassLoader classLoader;

    private JarFile(File jar) {
        this.jar = jar;
    }

    public static JarFile forFile(File jar) {
        if (jarFiles.containsKey(jar)) return jarFiles.get(jar);

        JarFile jarFile = new JarFile(jar);
        jarFiles.put(jar, jarFile);
        return jarFile;
    }

    public static ClassLoader createClassLoaderFromJars(@Nullable ClassLoader parentClassLoader, JarFile... jarFiles) {
        URL[] urls = new URL[jarFiles.length];
        for (int i = 0; i < jarFiles.length; ++i) {
            urls[i] = jarFiles[i].getUrl();
        }

        return new URLClassLoader(urls, parentClassLoader == null ? JarFile.class.getClassLoader() : parentClassLoader);
    }

    public ClassLoader classLoader() {
        if (classLoader != null) return classLoader;

        classLoader = new URLClassLoader(new URL[]{getUrl()}, this.getClass().getClassLoader());
        return classLoader;
    }

    private URL getUrl() {
        try {
            return jar.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format("Could not create classloader for \"%s\"", jar.getAbsolutePath()), e);
        }
    }

    public Set<JarClass<?>> getClasses(ClassLoader loader) throws IOException, ClassNotFoundException {
        java.util.jar.JarFile iJar = new java.util.jar.JarFile(jar);
        Enumeration<JarEntry> entries = iJar.entries();
        Set<JarClass<?>> jarClasses = new HashSet<>();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!name.endsWith(".class")) continue;
            JarClass jarClass = new JarClass(loader, this, name);
            findInnerClasses(jarClasses, jarClass);
            jarClasses.add(jarClass);
        }

        return jarClasses;
    }

    private void findInnerClasses(Set<JarClass<?>> jarClasses, JarClass<?> jarClass) {
        for (JarClass<?> innerKlazz : jarClass.innerClasses()) {
            jarClasses.add(innerKlazz);
            findInnerClasses(jarClasses, innerKlazz);
        }
    }
}

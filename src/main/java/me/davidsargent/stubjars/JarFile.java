package me.davidsargent.stubjars;

import me.davidsargent.stubjars.components.JarClass;
import org.jetbrains.annotations.NotNull;

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

    public static ClassLoader createClassLoaderFromJars(JarFile... jarFiles) {
        URL[] urls = new URL[jarFiles.length];
        for (int i = 0; i < jarFiles.length; ++i) {
            urls[i] = jarFiles[i].getUrl();
        }

        return new URLClassLoader(urls, JarFile.class.getClassLoader());
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

    private void findInnerClasses(Set<JarClass<?>> jarClasses, JarClass jarClass) {
        for (@NotNull JarClass<?> innerKlazz : jarClass.innerClasses()) {
            jarClasses.add(innerKlazz);
            findInnerClasses(jarClasses, innerKlazz);
        }
    }
}

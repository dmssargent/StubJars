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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static boolean shouldBuild = false;
    private static boolean shouldJar = false;

    public static void main(String... args) throws IOException {
        StubJars.Builder builder = StubJars.builder();
        parseArgs(builder, args);
        StubJars stubJars = buildStubJarsInstance(builder);
        createDirectoryTree(stubJars);
        boolean sourceFilesCreated = createSourceFiles(stubJars);
        if (!sourceFilesCreated) {
            log.error("Source file creation was interrupted due to an error");
            System.exit(1);
        }

        if (shouldBuild) {
            compileGeneratedCode(stubJars);
        }
        if (shouldJar) {
            generateStubsJarForGeneratedCode(stubJars);
        }

        log.info("StubJars has finished");
    }

    private static void parseArgs(StubJars.Builder builder, String[] args) throws IOException {
        List<File> files = new ArrayList<>(args.length);
        for (String argument : args) {
            if (argument.startsWith("-")) {
                parseArg(builder, argument);
                continue;
            }

            File e = new File(argument);
            if (!e.exists()) {
                log.error("The file \"{}\" does not exist!", argument);
            } else if (e.isDirectory()) {
                log.error("The file \"{}\" is a folder!", argument);
            } else if (!e.canRead()) {
                log.error("The file \"{}\" cannot be read!", argument);
            } else {
                files.add(e);
                continue;
            }

            log.error("Encountered an error loading the specified JAR files");
            System.exit(1);
        }

        builder.addJarsAndAars(files.toArray(new File[] {}));
    }

    @NotNull
    private static StubJars buildStubJarsInstance(StubJars.Builder builder) {
        log.info("Loading the JARs to be stubbed");
        StubJars build = builder.build();
        log.info("JAR load finished");
        return build;
    }

    private static void createDirectoryTree(StubJars build) {
        log.info("Creating the stub_src directory tree at \"{}\"", build.getSourceDestination().getAbsolutePath());
        build.createDirectoryTree();
        log.info("stub_src directory tree creation finished");
    }

    private static boolean createSourceFiles(StubJars build) {
        log.info("Starting creation of stub_src files");
        boolean sourceFilesCreated = build.createSourceFiles();
        log.info("Creation of stub_src files finished");
        if (!sourceFilesCreated) {
            log.error("Source file creation was interrupted due to an error");
        }
        return sourceFilesCreated;
    }

    private static void compileGeneratedCode(StubJars build) {
        log.info("Compiling stub_src files");
        try {
            build.compileGeneratedCode();
        } catch (InterruptedException ex) {
            log.warn("Compilation was interrupted");
            System.exit(1);
        } catch (IOException ex) {
            log.error("Failed to execute javac", ex);
            System.exit(1);
        }
        log.info("Compilation finished");
    }

    private static void generateStubsJarForGeneratedCode(StubJars build) {
        log.info("Compiling stub_src files");
        try {
            build.generateJarForGeneratedCode();
        } catch (InterruptedException ex) {
            log.warn("Compilation was interrupted");
            System.exit(1);
        } catch (IOException ex) {
            log.error("Failed to execute jar", ex);
            System.exit(1);
        }
        log.info("Compilation finished");
    }

    private static void parseArg(StubJars.Builder builder, String arg) throws IOException {
        if (arg.startsWith("-cp=")) {
            String path = arg.split("=", -1)[1];
            final String[] filePaths;
            if (path.contains(File.pathSeparator)) {
                filePaths = path.split(File.pathSeparator);
            } else {
                filePaths = new String[]{path};
            }

            for (String filePath : filePaths) {
                if (filePath.toLowerCase().endsWith(".jar")) {
                    builder.addClasspathJar(new File(filePath));
                } else if (filePath.toLowerCase().endsWith(".aar")) {
                    builder.addClasspathAar(new File(filePath));
                } else {
                    log.error("ignored: unknown classpath file: " + filePath);
                }
            }
        } else if (arg.equals("--build")) {
            shouldBuild = true;
        } else if (arg.equals("--jar")) {
            shouldJar = true;
        }
    }
}

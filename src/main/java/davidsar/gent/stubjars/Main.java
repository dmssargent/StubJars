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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {
        StubJars.Builder builder = StubJars.builder();
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

        builder.addJars(files.toArray(new File[] {}));
        log.info("Loading the JARs to be stubbed");
        StubJars build = builder.build();
        log.info("JAR load finished");
        log.info("Creating the stub_src directory tree at \"{}\"", build.getSourceDestination().getAbsolutePath());
        build.createDirectoryTree();
        log.info("stub_src directory tree creation finished");
        log.info("Starting creation of stub_src files");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        build.createSourceFiles();
        log.info("Creation of stub_src files finished");

        log.info("StubJars has finished");
    }

    private static void parseArg(StubJars.Builder builder, String arg) {
        if (arg.startsWith("-cp=")) {
            String path = arg.split("=", -1)[1];
            final String[] classpathJars;
            if (path.contains(File.pathSeparator)) {
                classpathJars = path.split(File.pathSeparator);
            } else {
                classpathJars = new String[]{path};
            }

            for (String jarPath : classpathJars) {
                builder.addClasspathJar(new File(jarPath));
            }
        }
    }
}

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class Main {
    public static void main(String... args) {
        StubJars.Builder builder = StubJars.builder();
        List<File> files = new ArrayList<>(args.length);
        for (String argument : args) {
            if (argument.startsWith("-")) {
                parseArg(builder, argument);
                continue;
            }

//            if (argument.contains("*")) {
//                String baseFolder = argument.startsWith("*") ? new File(".").getPath() : argument.substring(0, argument.lastIndexOf(File.separatorChar) + 1);
//                String regexToCheck = argument.substring(argument.lastIndexOf("/" + 1, argument.indexOf('*')));
//                regexToCheck = argument.substring(0, regexToCheck.indexOf("*") + 1);
//                regexToCheck = regexToCheck.replace("*", "\\.*");
//                List<String> matches = new ArrayList<>();
//                for (String possibleResult : Objects.requireNonNull(new File(baseFolder).list())) {
//                    if (possibleResult.matches(regexToCheck)) {
//                        matches.add(new File(Path));
//                    }
//                }
//            }

            File e = new File(argument);
            if (!e.exists()) System.err.println(String.format("The file \"%s\" does not exist!", argument));
            if (e.isDirectory()) System.err.println(String.format("The file \"%s\" is a folder!", argument));
            if (!e.canRead()) System.err.println(String.format("The file \"%s\" cannot be read!", argument));
            files.add(e);
        }
        builder.addJars(files.toArray(new File[] {}));
        System.out.print("Loading...");
        StubJars build = builder.build();
        System.out.println("done!");
        System.out.print("Building directory tree at \"" + build.getSourceDestination().getAbsolutePath() + "\"...");
        build.createDirectoryTree();
        System.out.println("done!");
        System.out.print("Writing source files...");
        build.createSourceFiles();
        System.out.println("done!");


        System.out.println("\nDone! Bye!");
    }

    private static void parseArg(StubJars.Builder builder, String arg) {
        if (arg.startsWith("-cp=")) {
            String path = arg.split("=")[1];
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

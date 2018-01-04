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

            File e = new File(argument);
            if (!e.exists()) System.err.println(String.format("The file \"%s\" does not exist!", argument));
            if (e.isDirectory()) System.err.println(String.format("The file \"%s\" is a folder!", argument));
            if (!e.canRead()) System.err.println(String.format("The file \"%s\" cannot be read!", argument));
            files.add(e);
        }
        builder.addJars(files.toArray(new File[] {}));
        System.out.print("Ready? ");
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

    }
}

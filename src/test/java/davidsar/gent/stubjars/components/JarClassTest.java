package davidsar.gent.stubjars.components;


import davidsar.gent.stubjars.components.expressions.Expression;
import davidsar.gent.stubjars.components.expressions.Expressions;
import davidsar.gent.stubjars.components.expressions.PackageStatement;
import davidsar.gent.stubjars.components.expressions.StringExpression;
import davidsar.gent.stubjars.components.writer.Constants;
import davidsar.gent.stubjars.components.writer.JavaClassWriter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class JarClassTest {
    @Test
    public void testInterfaceIsWritten() throws ClassNotFoundException {
        var testInterfaceClass = new JarClass<TestInterface>(JarClassTest.class.getClassLoader(), TestInterface.class.getName());
        assertThat(testInterfaceClass.isInterface()).isTrue();
        assertThat(testInterfaceClass.isEnum()).isFalse();
        assertThat(testInterfaceClass.isInnerClass()).isFalse();

        assertThat(testInterfaceClass.compileToExpression().toString()).isEqualTo("public interface TestInterface  {\n" +
            "                    default   void testDefaultMethod() {}\n" +
            "void testMethod();\n" +
            "    }\n");
    }

    @Test
    public void testImplementationIsWritten() throws ClassNotFoundException {
        var testInterfaceClass = new JarClass<TestImplementation>(JarClassTest.class.getClassLoader(), TestImplementation.class.getName());
        assertThat(testInterfaceClass.isInterface()).isFalse();
        assertThat(testInterfaceClass.isEnum()).isFalse();
        assertThat(testInterfaceClass.isInnerClass()).isFalse();

        assertThat(testInterfaceClass.compileToExpression().toString()).isEqualTo(
            "public class TestImplementation  implements TestInterface{\n" +
            "                public  TestImplementation() {}\n" +
            "        public    void testMethod() {}\n" +
                "    }\n");
    }

    @Test
    public void fieldDeclarationArrayWorks() throws ClassNotFoundException {
        var testInterfaceClass = new JarClass<TestConstructorClass>(JarClassTest.class.getClassLoader(), TestConstructorClass.class.getName());
        assertThat(testInterfaceClass.isInterface()).isFalse();
        assertThat(testInterfaceClass.isEnum()).isFalse();
        assertThat(testInterfaceClass.isInnerClass()).isFalse();

        assertThat(testInterfaceClass.compileToExpression().toString()).isEqualTo(
"public class TestConstructorClass  {\n" +
    "                public  TestConstructorClass() {}\n" +
    "                public enum ColorSwatch  {\n" +
    "    RED{\n" +
    "                        }\n" +
    ",\n" +
    "GREEN{\n" +
    "                        }\n" +
    ",\n" +
    "BLUE{\n" +
    "                        }\n" +
    ";\n" +
    "                    }\n" +
    "public enum EmptyEnum  {\n" +
    "    ;\n" +
    "                    }\n" +
    "public static class Result  {\n" +
    "        public final davidsar.gent.stubjars.components.TestConstructorClass.ColorSwatch closestSwatch = davidsar.gent.stubjars.components.TestConstructorClass.ColorSwatch.RED;\n" +
    "public final int[] rgb = new int[] {};\n" +
    "        public  Result(davidsar.gent.stubjars.components.TestConstructorClass.ColorSwatch arg0, float[] arg1) {}\n" +
    "            }\n" +
    "public static class ResultEmptyEnum  {\n" +
    "        public final davidsar.gent.stubjars.components.TestConstructorClass.EmptyEnum closestSwatch = null;\n" +
    "public final int[] rgb = new int[] {};\n" +
    "        public  ResultEmptyEnum(davidsar.gent.stubjars.components.TestConstructorClass.EmptyEnum arg0, float[] arg1) {}\n" +
    "            }\n" +
    "public static abstract class ResultGetter  {\n" +
    "                public  ResultGetter() {}\n" +
    "        public  abstract   davidsar.gent.stubjars.components.TestConstructorClass.Result getAnalysis();\n" +
    "    }\n" +
    "}\n");
        Expression packageStatement = new PackageStatement(testInterfaceClass.packageName());
        Expression classBody = testInterfaceClass.compileToExpression();
        Expression fullClass = Expressions.of(packageStatement, StringExpression.NEW_LINE, classBody);
        var result = String.join(Constants.NEW_LINE_CHARACTER, TreeFormatter.toLines(fullClass));
        assertThat(result).isNotEmpty();
    }
}
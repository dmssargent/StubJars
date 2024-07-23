package davidsar.gent.stubjars.components;


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
}
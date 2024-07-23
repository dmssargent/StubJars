package davidsar.gent.stubjars.components;


import davidsar.gent.stubjars.components.expressions.Expression;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class JarMethodTest {
    @Test
    public void testInterfaceIsWritten() throws ClassNotFoundException {
        var testInterfaceClass = new JarClass<TestInterface>(JarMethodTest.class.getClassLoader(), TestInterface.class.getName());
        assertThat(testInterfaceClass.isInterface()).isTrue();
        assertThat(testInterfaceClass.isEnum()).isFalse();
        assertThat(testInterfaceClass.isInnerClass()).isFalse();

        assertThat(testInterfaceClass.compileToExpression().toString()).isEqualTo("public interface TestInterface  {\n" +
            "                    default   void testDefaultMethod() {}void testMethod();\n" +
            "    }\n");
    }

    @Test
    public void testImplementationIsWritten() throws ClassNotFoundException {
        var testInterfaceClass = new JarClass<TestImplementation>(JarMethodTest.class.getClassLoader(), TestImplementation.class.getName());
        assertThat(testInterfaceClass.isInterface()).isFalse();
        assertThat(testInterfaceClass.isEnum()).isFalse();
        assertThat(testInterfaceClass.isInnerClass()).isFalse();

        assertThat(testInterfaceClass.compileToExpression().toString()).isEqualTo("public class TestImplementation  implements TestInterface{\n" +
"                public  TestImplementation() {}        public    void testMethod() {}    }\n");
    }
}
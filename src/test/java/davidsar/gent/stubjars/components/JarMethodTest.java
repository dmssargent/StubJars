package davidsar.gent.stubjars.components;


import davidsar.gent.stubjars.components.expressions.Expressions;
import davidsar.gent.stubjars.components.expressions.StringExpression;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class JarMethodTest {
    @Test
    public void testDefaultMethodInInterfaceInterfaceIsWritten() throws ClassNotFoundException, NoSuchMethodException {
        var testInterfaceClass = new JarClass<TestInterface>(JarMethodTest.class.getClassLoader(), TestInterface.class.getName());
        var method = new JarMethod(testInterfaceClass, testInterfaceClass.getClazz().getDeclaredMethod("testDefaultMethod"));
        assertThat(method.isAbstract()).isFalse();

        assertThat(TreeFormatter.toLines(method.compileToExpression())).containsExactly("default   void testDefaultMethod() {", "}");
    }

    @Test
    public void testMethodInInterfaceIsWritten() throws ClassNotFoundException, NoSuchMethodException {
        var testInterfaceClass = new JarClass<TestInterface>(JarMethodTest.class.getClassLoader(), TestInterface.class.getName());
        var method = new JarMethod(testInterfaceClass, testInterfaceClass.getClazz().getDeclaredMethod("testMethod"));
        assertThat(method.isAbstract()).isTrue();

        assertThat(TreeFormatter.toLines(method.compileToExpression())).containsExactly("void testMethod();");
    }

    @Test
    public void testMethodInImplementationIsWritten() throws ClassNotFoundException, NoSuchMethodException {
        var testInterfaceClass = new JarClass<TestInterface>(JarMethodTest.class.getClassLoader(), TestImplementation.class.getName());
        var method = new JarMethod(testInterfaceClass, testInterfaceClass.getClazz().getDeclaredMethod("testMethod"));
        assertThat(method.isAbstract()).isFalse();

        assertThat(TreeFormatter.toLines(method.compileToExpression())).containsExactly("public    void testMethod() {", "}");
    }
}
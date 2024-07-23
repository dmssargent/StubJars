package davidsar.gent.stubjars.components;

public interface TestInterface {
    void testMethod();

    default void testDefaultMethod() {
        System.out.println("[default] Hello World");
    }
}

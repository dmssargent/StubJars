package me.davidsargent.stubjars.components;

public enum SecurityModifier {
    PRIVATE("private"), PROTECTED("protected"), PACKAGE(""), PUBLIC("public");
    private final String modifier;

    SecurityModifier(String modifier) {
        this.modifier = modifier;
    }

    public String getModifier() {
        return modifier;
    }
}

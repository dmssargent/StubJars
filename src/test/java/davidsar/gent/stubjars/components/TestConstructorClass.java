package davidsar.gent.stubjars.components;

public class TestConstructorClass {
    public static abstract class ResultGetter {
        public abstract Result getAnalysis();
    }

    public enum ColorSwatch {
        RED, GREEN, BLUE
    }

    public static class Result
    {
        public final ColorSwatch closestSwatch;
        public final int[] rgb = new int[3];

        public Result(ColorSwatch closestSwatch, float[] hsv)
        {
            this.closestSwatch = closestSwatch;
        }
    }
}

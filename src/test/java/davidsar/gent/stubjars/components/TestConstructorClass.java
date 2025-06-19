package davidsar.gent.stubjars.components;

public class TestConstructorClass {
    public static abstract class ResultGetter {
        public abstract Result getAnalysis();
    }

    public enum ColorSwatch {
        RED, GREEN, BLUE
    }

    public enum EmptyEnum {
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

    public static class ResultEmptyEnum
    {
        public final EmptyEnum closestSwatch;
        public final int[] rgb = new int[3];

        public ResultEmptyEnum(EmptyEnum closestSwatch, float[] hsv)
        {
            this.closestSwatch = closestSwatch;
        }
    }
}

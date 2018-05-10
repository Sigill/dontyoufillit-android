package io.github.sigill.dontyoufillit;

public class Utils {
    public static float normalizeRadian(final float a) {
        final float TWOPI = (float) (2 * Math.PI);

        float aa = a % TWOPI;
        if (aa < 0) return aa + TWOPI;
        if (aa > TWOPI) return aa - TWOPI;
        return aa;
    }
}

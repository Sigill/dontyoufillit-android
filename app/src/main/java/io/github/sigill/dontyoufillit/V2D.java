package io.github.sigill.dontyoufillit;

public class V2D {
    public float x = 0, y = 0;

    public V2D() {
    }

    public V2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void normalize() {
        float length = mag();
        if(length > 0) {
            x /= length;
            y /= length;
        }
    }

    public void mult(float value) {
        float v = Math.signum(value) * (float) Math.sqrt(Math.abs(value));
        x *= v;
        y *= v;
    }

    public float mag() {
        return (float) Math.sqrt(x * x + y * y);
    }
}

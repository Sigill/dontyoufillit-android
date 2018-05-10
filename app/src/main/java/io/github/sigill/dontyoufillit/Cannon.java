package io.github.sigill.dontyoufillit;

public class Cannon extends RK41DObject {
    private final float PI_2 = (float)(Math.PI / 2);

    Cannon() {
        super();
        this.state.u = 0;
        this.state.s = (float)(Math.PI / 3);
    }

    @Override
    public float acceleration(State s, float t) { return 0; }

    /*
     * Angle of the cannon. 0 is up, -pi/2 is left, pi/2 is right.
     * WARNING: Opposite of the screen coordinates system!
     */
    public float getAngle() { return state.u + PI_2; }

    public void update(float t, float dt) {
        integrate(state, t, dt);

        if(Math.abs(state.u) >= PI_2) {
            state.u = (PI_2 - Math.abs(PI_2 - Math.abs(state.u))) * Math.signum(state.u);
            state.s *= -1;
        }
    }
}

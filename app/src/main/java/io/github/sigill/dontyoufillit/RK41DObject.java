package io.github.sigill.dontyoufillit;

public abstract class RK41DObject {
    class State {
        public float u, s;
        public State() {
            u = 0;
            s = 0;
        }

        public State(float _u, float _s) {
            u = _u;
            s = _s;
        }
    }

    class Derivative {
        public float du = 0.0f, ds = 0.0f;
        public Derivative() {}
    }

    private final static float one_sixth = 1/6.0f;

    protected final State state = new State();

    private final Derivative a = new Derivative();
    private final Derivative b = new Derivative();
    private final Derivative c = new Derivative();
    private final Derivative d = new Derivative();

    protected abstract float acceleration(final State s, final float t);

    private void evaluate(final State initial, final float t, Derivative out) {
        out.du = initial.s;
        out.ds = acceleration(initial, t);
    }

    private void evaluate(final State initial, final float t, final float dt, final Derivative d, Derivative out)
    {
        State state = new State(initial.u + d.du * dt, initial.s + d.ds * dt);
        out.du = state.s;
        out.ds = acceleration(state, t+dt);
    }

    public void integrate(final State state, final float t, final float dt)
    {
        evaluate(state, t, a);
        evaluate(state, t, dt * 0.5f, a, b);
        evaluate(state, t, dt * 0.5f, b, c);
        evaluate(state, t, dt, c, d);

        final float dudt = one_sixth * (a.du + 2.0f * (b.du + c.du) + d.du);
        final float dsdt = one_sixth * (a.ds + 2.0f * (b.ds + c.ds) + d.ds);

        state.u = state.u + dudt * dt;
        state.s = state.s + dsdt * dt;
    }
}

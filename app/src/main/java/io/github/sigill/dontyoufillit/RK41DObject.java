package io.github.sigill.dontyoufillit;

public abstract class RK41DObject {
    class State {
        public float u, s;
        public State() {
            u = 0;
            s = 0;
        }

        public State(State state) {
            u = state.u;
            s = state.s;
        }
    }

    class Derivative {
        public float du, ds;
        public Derivative() {
            du = 0;
            ds = 0;
        }
    }

    protected State state = new State();

    protected abstract float acceleration(State s, float t);

    private Derivative evaluate(State initial, float t) {
        Derivative output = new Derivative();
        output.du = initial.s;
        output.ds = acceleration(initial, t);
        return output;
    }

    private Derivative evaluate(State initial, float t, float dt, Derivative d)
    {
        State state = new State();
        state.u = initial.u + d.du * dt;
        state.s = initial.s + d.ds * dt;

        Derivative output = new Derivative();
        output.du = state.s;
        output.ds = acceleration(state, t+dt);
        return output;
    }

    public void integrate(State state, float t, float dt)
    {
        Derivative a = evaluate(state, t);
        Derivative b = evaluate(state, t, dt * 0.5f, a);
        Derivative c = evaluate(state, t, dt * 0.5f, b);
        Derivative d = evaluate(state, t, dt, c);

        final float dudt = 1.0f/6.0f * (a.du + 2.0f * (b.du + c.du) + d.du);
        final float dsdt = 1.0f/6.0f * (a.ds + 2.0f * (b.ds + c.ds) + d.ds);

        state.u = state.u + dudt * dt;
        state.s = state.s + dsdt * dt;
    }
}

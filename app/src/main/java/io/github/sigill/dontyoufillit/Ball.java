package io.github.sigill.dontyoufillit;

import java.util.List;

public class Ball extends RK41DObject {
    public float nr; // Normalized radius
    public float nx, ny; // Normalized coordinates
    public float direction; // Direction in radians
    public int counter;

    public Ball(float _r, float _x, float _y, float _a) {
        this.nr = _r;
        this.nx = _x;
        this.ny = _y;

        this.counter = 3;

        this.direction = _a;
        this.state.u = 0;
        this.state.s = 1f;
    }

    @Override
    protected float acceleration(final State s, final float t) { return -0.4f; }

    void update(float t, float dt, List<Ball> staticBalls) {
        float _u = state.u;

        integrate(state, t, dt);

        float d = state.u - _u;

        nx += d * Math.cos(direction);
        ny += d * Math.sin(direction);

        bounce(staticBalls);
    }

    private void bounce(final List<Ball> staticBalls) {
        if (this.nx > 1 - this.nr) {
            this.nx = 1 - this.nr;
            this.direction = Utils.normalizeRadian((float)(Math.PI - direction));
        } else if (this.nx < this.nr) {
            this.nx = this.nr;
            this.direction = Utils.normalizeRadian((float)(Math.PI - direction));
        }

        if (this.ny > 1 - this.nr) {
            this.ny = 1 - this.nr;
            this.direction = Utils.normalizeRadian(-this.direction);
        }

        V2D normal = new V2D();

        for(final Ball o : staticBalls) {
            // Vector joining the two balls
            normal.x = this.nx - o.nx;
            normal.y = this.ny - o.ny;

            float dist = normal.mag();
            if(normal.mag() <= o.nr + this.nr) {
                --o.counter;

                // Move it back to prevent clipping
                this.nx = o.nx + normal.x * (this.nr + o.nr) / dist;
                this.ny = o.ny + normal.y * (this.nr + o.nr) / dist;

                // http://en.wikipedia.org/wiki/Elastic_collision#Two-Dimensional_Collision_With_Two_Moving_Objects
                // Assuming no speed and an infinite mass for the second ball.
                float phi = (float)Math.atan2(normal.y, normal.x),
                    theta = this.direction,
                    speed = this.state.s;

                double velocityX = -speed * Math.cos(theta - phi) * Math.cos(phi) + speed * Math.sin(theta - phi) * Math.cos(phi + Math.PI / 2),
                       velocityY = -speed * Math.cos(theta - phi) * Math.sin(phi) + speed * Math.sin(theta - phi) * Math.sin(phi + Math.PI / 2);

                // Linear speed doesn't change, only the direction.
                this.direction = (float)Math.atan2(velocityY, velocityX);
            }
        }
    }

    public void grow(final List<Ball> staticBalls) {
        float minRadius = Float.MAX_VALUE;
        float available;
        V2D vector = new V2D();

        for (final Ball o : staticBalls) {
            vector.x = this.nx - o.nx;
            vector.y = this.ny - o.ny;

            available = vector.mag() - o.nr;
            if(minRadius > available) minRadius = available;
        }

        available = this.nx;
        if(minRadius > available) minRadius = available;

        available = 1 - this.nx;
        if(minRadius > available) minRadius = available;

        available = Math.abs(this.ny);
        if(minRadius > available) minRadius = available;

        available = Math.abs(1 - this.ny);
        if(minRadius > available) minRadius = available;

        this.nr = Math.abs(minRadius);
    }
}

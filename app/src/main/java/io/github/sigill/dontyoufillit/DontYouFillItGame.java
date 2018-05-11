package io.github.sigill.dontyoufillit;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;

public class DontYouFillItGame extends Observable {
    public enum State { PAUSED, RUNNING, GAMEOVER }
    public State state;
    public Cannon cannon;
    public Ball currentBall;
    public List<Ball> staticBalls;
    public int score;
    private long lastUpdateTime;

    public static final float DEFAULT_BALL_RADIUS =  1/40.0f;
    public static final float CANNON_Y_POSITION   = -1/6.0f;
    public static final float CANNON_BASE_HEIGHT  =  1/15.0f;
    public static final float CANNON_LENGTH       =  1/15.0f;

    public DontYouFillItGame() {
        state = State.PAUSED;
        cannon = new Cannon();
        currentBall = null;
        staticBalls = new ArrayList<>();
        score = 0;
        lastUpdateTime = System.currentTimeMillis();
    }

    public void pause() {
        state = State.PAUSED;
    }

    public void resume() {
        state = State.RUNNING;
        lastUpdateTime = System.currentTimeMillis();
    }

    public void reset() {
        currentBall = null;
        staticBalls.clear();
        score = 0;
        resume();
    }

    public void update(long time) {
//        Log.v("UPDATE", "at: " + time);
        if (this.currentBall != null) {
            long last = this.lastUpdateTime,
                steps = time - this.lastUpdateTime,
              current;
            for(int i = 1; i <= steps; ++i) {
                current = (this.lastUpdateTime * (steps-i) + time * i) / steps;
                this.currentBall.update(last / 1000f, (current - last) / 1000f, this.staticBalls);

                Iterator<Ball> itr = this.staticBalls.iterator();
                while (itr.hasNext()) {
                    Ball o = itr.next();
                    if(o.counter == 0) {
                        ++this.score;
                        itr.remove();
                    }
                }

                if(this.currentBall.ny < this.currentBall.nr && Utils.normalizeRadian(this.currentBall.direction) > Math.PI) {
                    this.currentBall.state.s = 0;
                    this.state = State.GAMEOVER;
                    setChanged();
                    notifyObservers();
                    break;
                } else if(this.currentBall.state.s < 0.001) {
                    if(this.currentBall.ny >= 0) {
                        this.currentBall.grow(this.staticBalls);
                        this.staticBalls.add(this.currentBall);
                    }
                    this.currentBall = null;
                    break;
                }
                last = current;
            }
        }

        this.cannon.update(this.lastUpdateTime / 1000f, (time - this.lastUpdateTime) / 1000f);

        this.lastUpdateTime = time;
    }

    public void fire() {
        this.currentBall = new Ball(
                1 / 40.0f,
                0.5f + (float) Math.cos(this.cannon.getAngle()) * CANNON_LENGTH,
                -1 / 6.0f + CANNON_BASE_HEIGHT + (float) Math.sin(this.cannon.getAngle()) * CANNON_LENGTH,
                this.cannon.getAngle());
    }
}
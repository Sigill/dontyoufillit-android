package io.github.sigill.dontyoufillit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;

public class DontYouFillItGame {
    public enum State { PAUSED, RUNNING, GAMEOVER }
    private List<DontYouFillItGameListener> mListeners;
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
        mListeners = new ArrayList<>();
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
        if (this.currentBall != null) {
            long last = this.lastUpdateTime,
                steps = time - this.lastUpdateTime,
              current;
            for(int i = 1; i <= steps; ++i) {
                current = (this.lastUpdateTime * (steps-i) + time * i) / steps;
                this.currentBall.update((current - last) / 1000f, this.staticBalls);

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
                    for (DontYouFillItGameListener listener : mListeners) {
                        listener.onGameOver();
                    }
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

        this.cannon.update((time - this.lastUpdateTime) / 1000f);

        this.lastUpdateTime = time;
    }

    public void fire() {
        this.currentBall = new Ball(
                DEFAULT_BALL_RADIUS,
                0.5f + (float) Math.cos(this.cannon.getAngle()) * CANNON_LENGTH,
                CANNON_Y_POSITION + CANNON_BASE_HEIGHT + (float) Math.sin(this.cannon.getAngle()) * CANNON_LENGTH,
                this.cannon.getAngle());
    }

    public void addListener(DontYouFillItGameListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeListener(DontYouFillItGameListener listener) {
        mListeners.remove(listener);
    }
}

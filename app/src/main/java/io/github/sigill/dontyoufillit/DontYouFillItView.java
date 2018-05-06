package io.github.sigill.dontyoufillit;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import java.util.ArrayList;
import java.util.Iterator;

public class DontYouFillItView extends View implements OnTouchListener {
    protected static final int FPS = 60;
    private static final String HIGHSCORE_PREF = "highscore";

    public static enum BallState {Launched, Alive, Stopped}

    public static enum Mode {PAUSE, RUNNING, GAMEOVER}
    private Mode mCurrentMode = Mode.RUNNING;

    private float SCALE, GAME_WIDTH, GAME_HEIGHT, V_OFFSET, H_OFFSET,
    BOTTOM_BORDER, TOP_BORDER, LEFT_BORDER, RIGHT_BORDER,
    CANNON_BASE_WIDTH, CANNON_BASE_HEIGHT, CANNON_LENGTH, CANNON_WIDTH;

    /** Keeps the game thread alive */
    private boolean mContinue = true;

    /** Timestamp of the last frame created */
    private long mLastFrameTimestamp = 0, mCurrentFrameTimestamp = 0, mLastTouchTimestamp = 0;

    /** Paint object */
    private final Paint mPaint = new Paint();

    private Cannon mCannon = new Cannon();
    private ArrayList<Ball> mListBalls = new ArrayList<Ball>();
    private Ball mCurrentBall = null;
    private int mScore = 0;
    private int mHighScore = 0;

    private int fpsCounter = 0;
    private long fpsCounterStart = System.currentTimeMillis();

    private int updateCounter = 0;
    private long updateCounterStart = System.currentTimeMillis();

    private RefreshHandler mRedrawHandler = new RefreshHandler();

    DontYouFillItView(Context context) {
        super(context);
        initView();
    }

    public DontYouFillItView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DontYouFillItView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        setOnTouchListener(this);
        setFocusable(true);

        Context ctx = this.getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        this.mHighScore = prefs.getInt(HIGHSCORE_PREF, 0);

        mPaint.setAntiAlias(true);

        resetGame();
    }

    private void resetGame() {
        this.mCurrentBall = null;
        this.mListBalls.clear();
        this.mScore = 0;
    }

    protected void computeLayout() {
        float w = this.getWidth(), h = this.getHeight();

        if(w / h < 3.0f/4.0f) {
            this.GAME_WIDTH = w;
            this.GAME_HEIGHT = 4.0f / 3.0f * GAME_WIDTH;
        } else {
            this.GAME_HEIGHT = h;
            this.GAME_WIDTH = 3.0f / 4.0f * GAME_HEIGHT;
        }
        this.SCALE = this.GAME_WIDTH;

        this.V_OFFSET = (h - this.GAME_HEIGHT) / 2.0f;
        this.H_OFFSET = (w - this.GAME_WIDTH) / 2.0f;

        this.TOP_BORDER = this.V_OFFSET + this.SCALE/6.0f;
        this.BOTTOM_BORDER = this.TOP_BORDER + this.SCALE;
        this.LEFT_BORDER = this.H_OFFSET;
        this.RIGHT_BORDER = this.LEFT_BORDER + this.SCALE;

        this.CANNON_BASE_WIDTH = this.SCALE / 10.0f;
        this.CANNON_BASE_HEIGHT = this.SCALE / 15.0f;
        this.CANNON_LENGTH = this.SCALE / 15.0f;
        this.CANNON_WIDTH = this.SCALE / 18.0f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeLayout();
    }

    public void setMode(Mode newMode) {
        mCurrentMode = newMode;

        if (mCurrentMode == Mode.RUNNING) {
            mContinue = true;
            update();
        } else {
            mContinue = false;
        }
    }

    @Override
    public boolean onTouch(View arg0, MotionEvent e) {
        long previousTouchTimestamp = this.mLastTouchTimestamp;
        this.mLastTouchTimestamp = System.currentTimeMillis();

        if(previousTouchTimestamp > this.mLastTouchTimestamp - 500)
            return true;

        //Log.v("Touch", "X = " + e.getX() + ", Y = " + e.getY());

        if(this.mCurrentMode == Mode.GAMEOVER) {
            resetGame();
            resume();
            return true;
        }

        if(this.mCurrentMode == Mode.RUNNING && (e.getX() > getWidth() - 60) && (e.getY() < 60)) {
            pause();
            return true;
        }

        if(this.mCurrentMode == Mode.RUNNING && this.mCurrentBall == null) {
            this.mCurrentBall = new Ball(
                    1 / 40.0f,
                    0.5f + (float) Math.cos(this.mCannon.getAngle()) * CANNON_LENGTH / SCALE,
                    -1 / 6.0f + CANNON_BASE_HEIGHT / SCALE + (float) Math.sin(this.mCannon.getAngle()) * CANNON_LENGTH / SCALE,
                    this.mCannon.getAngle());
            return true;
        }

        if(this.mCurrentMode == Mode.PAUSE) {
            resume();
            return true;
        }

        return true;
    }

    public void update() {
        if(getHeight() == 0 || getWidth() == 0) {
            mRedrawHandler.sleep(1000 / FPS);
            return;
        }

        mCurrentFrameTimestamp = System.currentTimeMillis();

        if(this.mCurrentBall != null) {
            long last = mLastFrameTimestamp, current;
            for(int i = 1; i <= 10; ++i) {
                current = (mLastFrameTimestamp * (10-i) + mCurrentFrameTimestamp * i) / 10;

                this.mCurrentBall.update(last / 1000f, (current - last) / 1000f);

                if(this.mCurrentBall.ny < this.mCurrentBall.nr && normalizeRadian(this.mCurrentBall.direction) > Math.PI) {
                    this.mCurrentBall.state.s = 0f;

                    if(this.mScore > this.mHighScore) {
                        this.mHighScore = this.mScore;

                        Context ctx = this.getContext();
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(HIGHSCORE_PREF, this.mHighScore);
                        editor.commit();
                    }

                    setMode(Mode.GAMEOVER);
                }

                if(this.mCurrentBall.state.s < 0.01) {
                    if(this.mCurrentBall.ny >= 0) {
                        this.mCurrentBall.grow();
                        this.mListBalls.add(this.mCurrentBall);
                    }
                    this.mCurrentBall = null;

                    break;
                }

                last = current;
            }
        }

        mCannon.update(mLastFrameTimestamp / 1000f, (mCurrentFrameTimestamp - mLastFrameTimestamp) / 1000f);

        mLastFrameTimestamp = mCurrentFrameTimestamp;

        long now = System.currentTimeMillis();
        if(now - this.updateCounterStart < 2000) {
            this.updateCounter++;
        } else {
            Log.v("UPDATE_COUNTER", (int)(this.updateCounter / ((now - this.updateCounterStart) / 1000f)) + "ups");
            this.updateCounterStart = now;
            this.updateCounter = 0;
        }

        // We will take this much time off of the next update() call to normalize for
        // CPU time used updating the game state.
        if(mContinue) {
            long diff = System.currentTimeMillis() - mCurrentFrameTimestamp;
            mRedrawHandler.sleep(Math.max(0, (1000 / FPS) - diff));
        }
    }

    public void resume() {
        mLastFrameTimestamp = System.currentTimeMillis();
        mCurrentFrameTimestamp = mLastFrameTimestamp;

        setMode(Mode.RUNNING);
    }

    public void pause() {
        setMode(Mode.PAUSE);
    }

    /**
     * Paints the game!
     */
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(this.mCurrentMode == Mode.GAMEOVER) {
            float scoreOffset = mPaint.measureText("Game Over");
            canvas.drawText("Game Over", (getWidth() - scoreOffset) / 2f, getHeight() / 2f, this.mPaint);

            return;
        }

        drawScene(canvas);

        mCannon.draw(canvas);

        Iterator<Ball> itr = this.mListBalls.iterator();
        while(itr.hasNext())
            itr.next().draw(canvas);

        if(this.mCurrentBall != null)
            this.mCurrentBall.draw(canvas);

        long now = System.currentTimeMillis();
        if(now - this.fpsCounterStart < 2000) {
            this.fpsCounter++;
        } else {
            Log.v("FPS_COUNTER", (int)(this.fpsCounter / ((now - this.fpsCounterStart) / 1000f)) + "fps");
            this.fpsCounterStart = now;
            this.fpsCounter = 0;
        }
    }

    private void drawScene(Canvas canvas) {
        //mPaint.setColor(Color.DKGRAY);
        //canvas.drawRect(H_OFFSET, V_OFFSET,
        //                H_OFFSET + SCALE, BOTTOM_BORDER + SCALE/6.0f, mPaint);

        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(2);

        Path mainPath = new Path();
        mainPath.moveTo(this.LEFT_BORDER, this.BOTTOM_BORDER);
        mainPath.lineTo(this.LEFT_BORDER, this.TOP_BORDER);
        mainPath.lineTo(this.RIGHT_BORDER, this.TOP_BORDER);
        mainPath.lineTo(this.RIGHT_BORDER, this.BOTTOM_BORDER);
        canvas.drawPath(mainPath, mPaint); // Bottom

        Path bottomPath = new Path();
        bottomPath.moveTo(this.LEFT_BORDER, this.BOTTOM_BORDER);
        bottomPath.lineTo(this.RIGHT_BORDER, this.BOTTOM_BORDER);
        Paint dashedPaint = new Paint(mPaint);
        dashedPaint.setPathEffect(new DashPathEffect(new float[] {2, 2}, 0));
        canvas.drawPath(bottomPath, dashedPaint); // Bottom

        mPaint.setStyle(Style.FILL);
        mPaint.setStrokeWidth(0);
        if(this.mCurrentMode == Mode.RUNNING) {
            canvas.drawRect(getWidth() - 40, 10,
                    getWidth() - 30, 40, mPaint);

            canvas.drawRect(getWidth() - 20, 10,
                    getWidth() - 10, 40, mPaint);
        } else if(this.mCurrentMode == Mode.PAUSE) {
            Path path = new Path();
            path.setFillType(Path.FillType.EVEN_ODD);
            path.moveTo(getWidth() - 40, 10);
            path.lineTo(getWidth() - 40, 40);
            path.lineTo(getWidth() - 10, 25);
            path.close();
            canvas.drawPath(path, mPaint);
        }

        mPaint.setColor(Color.WHITE);
        mPaint.setTextSize((float) (this.SCALE/12.0));

        FontMetrics fmi = mPaint.getFontMetrics();

        canvas.drawText("Highscore", LEFT_BORDER, (float)(V_OFFSET + this.SCALE/12.0 - fmi.descent), mPaint);
        canvas.drawText("Score", LEFT_BORDER, (float)(V_OFFSET + this.SCALE/6.0 - fmi.descent), mPaint);

        float scoreOffset = mPaint.measureText("Highscore ");
        canvas.drawText(String.valueOf(this.mHighScore), LEFT_BORDER + scoreOffset, (float)(V_OFFSET + this.SCALE/12.0 - fmi.descent), mPaint);
        canvas.drawText(String.valueOf(this.mScore), LEFT_BORDER + scoreOffset, (float)(V_OFFSET + this.SCALE/6.0 - fmi.descent), mPaint);
    }

    class RefreshHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            DontYouFillItView.this.update();
            DontYouFillItView.this.invalidate(); // Mark the view as 'dirty'
        }

        public void sleep(long delay) {
            this.removeMessages(0);
            this.sendMessageDelayed(obtainMessage(0), delay);
        }
    }

    private class Cannon extends RK41DObject {
        public Cannon() {
            state.u = 0;
            state.s = (float) (Math.PI/3.0);
        }

        /*
         * Angle of the cannon. 0 is up, -pi/2 is left, pi/2 is right.
         * WARNING: Opposite of the screen coordinates system !
         */
        public float getAngle() {
            return state.u + (float)(Math.PI / 2.0);
        }

        @Override
        protected float acceleration(State s, float t) {
            return 0;
        }

        public void update(float t, float dt) {
            integrate(state, t, dt);
            if(Math.abs(state.u) >= Math.PI / 2.0f) {
                state.u = (float)(
                        (
                                (Math.PI / 2.0f)
                                - Math.abs(Math.PI / 2.0f - Math.abs(state.u))
                                )
                                * Math.signum(state.u));
                state.s *= -1;
            }
        }

        public void draw(Canvas canvas) {
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Style.FILL);

            canvas.drawRect(
                    H_OFFSET + (SCALE - CANNON_BASE_WIDTH) / 2.0f,
                    BOTTOM_BORDER + SCALE / 6.0f,
                    H_OFFSET + (SCALE - CANNON_BASE_WIDTH) / 2.0f + CANNON_BASE_WIDTH,
                    BOTTOM_BORDER + SCALE / 6.0f - CANNON_BASE_HEIGHT,
                    mPaint
                    );

            canvas.drawOval(
                    new RectF(
                            H_OFFSET + SCALE / 2.0f - CANNON_BASE_WIDTH / 2.0f,
                            (BOTTOM_BORDER + SCALE / 6.0f - CANNON_BASE_HEIGHT) - CANNON_BASE_WIDTH / 2.0f,
                            H_OFFSET + SCALE / 2.0f + CANNON_BASE_WIDTH / 2.0f,
                            (BOTTOM_BORDER + SCALE / 6.0f - CANNON_BASE_HEIGHT) + CANNON_BASE_WIDTH / 2.0f
                            ),
                            mPaint
                    );

            mPaint.setStyle(Style.FILL);
            mPaint.setStrokeWidth(CANNON_WIDTH);
            mPaint.setStrokeCap(Paint.Cap.BUTT);

            canvas.drawLine(
                    H_OFFSET + SCALE / 2.0f,
                    BOTTOM_BORDER + SCALE / 6.0f - CANNON_BASE_HEIGHT,
                    H_OFFSET + SCALE / 2.0f + (float) Math.cos(getAngle()) * CANNON_LENGTH, // End of cannon
                    BOTTOM_BORDER + SCALE / 6.0f - CANNON_BASE_HEIGHT - (float) Math.sin(getAngle()) * CANNON_LENGTH, // End of cannon
                    mPaint
                    );
        }
    }

    /*
    private float normalizeDegree(final float a) {
        float aa = a % 360;
        if (aa < 0) return aa + 360;
        if (aa > 360) return aa - 360;
        return aa;
    }
     */

    private float normalizeRadian(final float a) {
        final float TWOPI = (float) (2 * Math.PI);

        float aa = a % TWOPI;
        if (aa < 0) return aa + TWOPI;
        if (aa > TWOPI) return aa - TWOPI;
        return aa;
    }

    public class Ball extends RK41DObject {
        public float nr, nx, ny, // Normalized radius and coordinates
        direction;
        public int counter = 3;

        public Ball(float _r, float _x, float _y, float _a) {
            this.nr = _r;
            this.nx = _x;
            this.ny = _y;

            this.direction = _a;
            this.state.u = 0;
            this.state.s = 1f;
        }

        public void draw(Canvas canvas) {
            float x = LEFT_BORDER + this.nx * SCALE;
            float y = BOTTOM_BORDER - this.ny * SCALE;
            float r = this.nr * SCALE;

            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Style.FILL);
            mPaint.setStrokeWidth(0);

            canvas.drawOval(new RectF(x-r, y-r, x + r, y + r), mPaint);

            mPaint.setAntiAlias(false);

            switch(this.counter) {
            case 1: {
                mPaint.setColor(Color.BLACK);
                canvas.drawRect(
                        x-r*0.2f,
                        y-r*0.7f,
                        x+r*0.2f,
                        y+r*0.7f,
                        mPaint);
            } break;
            case 2: {
                mPaint.setColor(Color.BLACK);
                canvas.drawRect(
                        x-r*0.5f,
                        y-r*0.7f,
                        x+r*0.5f,
                        y+r*0.7f,
                        mPaint);
                mPaint.setColor(Color.WHITE);
                canvas.drawRect(
                        x-r*0.5f,
                        y-r*0.3f,
                        x+r*0.1f,
                        y-r*0.15f,
                        mPaint);
                canvas.drawRect(
                        x+r*0.5f,
                        y+r*0.3f,
                        x-r*0.1f,
                        y+r*0.15f,
                        mPaint);
            } break;
            case 3: {
                mPaint.setColor(Color.BLACK);
                canvas.drawRect(
                        x-r*0.5f,
                        y-r*0.7f,
                        x+r*0.5f,
                        y+r*0.7f,
                        mPaint);
                mPaint.setColor(Color.WHITE);
                canvas.drawRect(
                        x-r*0.5f,
                        y-r*0.3f,
                        x+r*0.1f,
                        y-r*0.15f,
                        mPaint);
                canvas.drawRect(
                        x-r*0.5f,
                        y+r*0.3f,
                        x+r*0.1f,
                        y+r*0.15f,
                        mPaint);
            } break;
            }
            mPaint.setAntiAlias(true);
        }

        @Override
        protected float acceleration(State s, float t) {
            return -0.4f;
        }

        void update(float t, float dt) {
            State previousState = new State(state);

            integrate(state, t, dt);

            float d = state.u - previousState.u;

            nx += d * Math.cos(direction);
            ny += d * Math.sin(direction);

            bounce();
        }

        public void bounce() {
            if (this.nx > 1 - this.nr) {
                this.nx = 1 - this.nr;
                this.direction = normalizeRadian((float)(Math.PI - direction));
            } else if (this.nx < this.nr) {
                this.nx = this.nr;
                this.direction = normalizeRadian((float)(Math.PI - direction));
            }

            if (this.ny > 1 - this.nr) {
                this.ny = 1 - this.nr;
                this.direction = normalizeRadian((float)(-this.direction));
            }

            Iterator<Ball> itr = mListBalls.iterator();

            while(itr.hasNext()) {
                Ball o = itr.next();

                // Vector joining the two balls
                V2D normal = new V2D(this.nx - o.nx, this.ny - o.ny);
                if(normal.mag() <= o.nr + this.nr) {
                    --o.counter;

                    float alpha = (float) Math.atan2(normal.y, normal.x);
                    float sine = (float) Math.sin(alpha);
                    float cosine = (float) Math.cos(alpha);

                    //Log.v("ALPHA", "" + (int)(alpha * 180 / Math.PI));
                    //this.ballState = BallState.Stopped;

                    //state.s = 0;

                    V2D velocity = new V2D((float) Math.cos(this.direction), (float) Math.sin(this.direction));

                    V2D bTemp = new V2D(
                            cosine * normal.x + sine * normal.y,
                            cosine * normal.y - sine * normal.x
                            );

                    V2D vTemp = new V2D(
                            cosine * velocity.x + sine * velocity.y,
                            cosine * velocity.y - sine * velocity.x
                            );

                    V2D vFinal = new V2D(-vTemp.x, vTemp.y);

                    bTemp.x += vFinal.x / SCALE;

                    V2D bFinal = new V2D();
                    bFinal.x = cosine * bTemp.x - sine * bTemp.y;
                    bFinal.y = cosine * bTemp.y + sine * bTemp.x;

                    this.nx = o.nx + bFinal.x;
                    this.ny = o.ny + bFinal.y;

                    velocity.x = cosine * vFinal.x - sine * vFinal.y;
                    velocity.y = cosine * vFinal.y + sine * vFinal.x;

                    this.direction = (float) Math.atan2(velocity.y, velocity.x);

                    if(o.counter == 0) {
                        mScore += 1;
                        itr.remove();
                    }
                }
            }
        }

        public void grow() {
            Iterator<Ball> itr = mListBalls.iterator();
            float minRadius = Float.MAX_VALUE;
            float available;
            Ball o;
            V2D vector;

            while(itr.hasNext()) {
                o = itr.next();
                vector = new V2D(mCurrentBall.nx - o.nx, mCurrentBall.ny - o.ny);
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
}

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
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import java.util.Observable;
import java.util.Observer;

public class DontYouFillItView extends View implements OnTouchListener, Observer, Choreographer.FrameCallback {
    private static final String HIGHSCORE_PREF = "highscore";

    DontYouFillItGame game = null;

    public enum Mode {PAUSE, RUNNING, GAMEOVER}
    private Mode mCurrentMode = Mode.RUNNING;

    private float SCALE, GAME_WIDTH, GAME_HEIGHT, V_OFFSET, H_OFFSET,
    BOTTOM_BORDER, TOP_BORDER, LEFT_BORDER, RIGHT_BORDER,
    CANNON_BASE_WIDTH, CANNON_BASE_HEIGHT, CANNON_LENGTH, CANNON_WIDTH;

    /** Timestamp of the last frame created */
    private long mLastFrameTimestamp = 0, mLastTouchTimestamp = 0;

    /** Paint object */
    private final Paint mPaint = new Paint();
    private final PathEffect mDottedEffect = new DashPathEffect(new float[] {2, 2}, 0);
    private final Path mMainBorder = new Path(), mBottomBorder = new Path();
    private FontMetrics mFontMetric = null;

    private int mHighScore = 0;

    private int fpsCounter = 0;
    private long fpsCounterStart = System.currentTimeMillis();

    public DontYouFillItView(Context context) {
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

        game = new DontYouFillItGame();
        game.addObserver(this);

        Context ctx = this.getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        this.mHighScore = prefs.getInt(HIGHSCORE_PREF, 0);

        mPaint.setAntiAlias(true);

        reset();
    }

    public void setMode(Mode newMode) {
        mCurrentMode = newMode;

        if (mCurrentMode == Mode.RUNNING) {
            Choreographer.getInstance().postFrameCallback(this);
        } else {
            Choreographer.getInstance().removeFrameCallback(this);
        }
    }

    private void reset() {
        game.reset();
        resume();
    }

    public void resume() {
        mLastFrameTimestamp = System.currentTimeMillis();

        this.fpsCounter = 0;
        this.fpsCounterStart = mLastFrameTimestamp;

        game.resume();

        setMode(Mode.RUNNING);
    }

    public void pause() {
        setMode(Mode.PAUSE);
    }

    @Override
    public void doFrame(long l) {
        invalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void update(Observable observable, Object o) {
        switch(game.state) {
            case GAMEOVER:
                if(game.score > this.mHighScore) {
                    this.mHighScore = game.score;

                    Context ctx = this.getContext();
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(HIGHSCORE_PREF, this.mHighScore);
                    editor.commit();
                }
                setMode(Mode.GAMEOVER);
                break;
        }
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

        mMainBorder.reset();
        mMainBorder.moveTo(this.LEFT_BORDER, this.BOTTOM_BORDER);
        mMainBorder.lineTo(this.LEFT_BORDER, this.TOP_BORDER);
        mMainBorder.lineTo(this.RIGHT_BORDER, this.TOP_BORDER);
        mMainBorder.lineTo(this.RIGHT_BORDER, this.BOTTOM_BORDER);

        mBottomBorder.reset();
        mBottomBorder.moveTo(this.LEFT_BORDER, this.BOTTOM_BORDER);
        mBottomBorder.lineTo(this.RIGHT_BORDER, this.BOTTOM_BORDER);

        mFontMetric = mPaint.getFontMetrics();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeLayout();
    }

    @Override
    public boolean onTouch(View arg0, MotionEvent e) {
        long previousTouchTimestamp = this.mLastTouchTimestamp;
        this.mLastTouchTimestamp = System.currentTimeMillis();

        if(previousTouchTimestamp > this.mLastTouchTimestamp - 500)
            return true;

//        Log.v("Touch", "X = " + e.getX() + ", Y = " + e.getY());

        if(this.mCurrentMode == Mode.GAMEOVER) {
            reset();
            return true;
        }

        if(this.mCurrentMode == Mode.RUNNING && (e.getX() > getWidth() - 60) && (e.getY() < 60)) {
            pause();
            return true;
        }

        if(this.mCurrentMode == Mode.RUNNING && game.currentBall == null) {
            game.fire();
            return true;
        }

        if(this.mCurrentMode == Mode.PAUSE) {
            resume();
            return true;
        }

        return true;
    }

    /**
     * Paints the game!
     */
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final long now = System.currentTimeMillis();
        final long elapsed = now - this.fpsCounterStart;
        this.fpsCounter++;

        if (elapsed >= 2000) {
            Log.v("FPS_COUNTER", (1000 * this.fpsCounter / (elapsed)) + "fps");
            this.fpsCounterStart = now;
            this.fpsCounter = 0;
        }

        if (this.mCurrentMode == Mode.RUNNING) {
            game.update(now);
        }

        mLastFrameTimestamp = now;

        if (this.mCurrentMode == Mode.GAMEOVER) {
            mPaint.setTextAlign(Paint.Align.CENTER);
//            float scoreOffset = mPaint.measureText("Game Over");
            canvas.drawText("Game Over", getWidth() / 2f, getHeight() / 2f, this.mPaint);
            mPaint.setTextAlign(Paint.Align.LEFT);

            return;
        }

        drawScene(canvas);

        draw(game.cannon, canvas);

        for (final Ball b : game.staticBalls)
            draw(b, canvas);

        if(game.currentBall != null)
            draw(game.currentBall, canvas);
    }

    private void drawScene(Canvas canvas) {
        //mPaint.setColor(Color.DKGRAY);
        //canvas.drawRect(H_OFFSET, V_OFFSET,
        //                H_OFFSET + SCALE, BOTTOM_BORDER + SCALE/6.0f, mPaint);

        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(2);

        canvas.drawPath(mMainBorder, mPaint);

        mPaint.setPathEffect(mDottedEffect);
        canvas.drawPath(mBottomBorder, mPaint); // Bottom
        mPaint.setPathEffect(null);

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

        canvas.drawText("Highscore", LEFT_BORDER, (float)(V_OFFSET + this.SCALE/12.0 - mFontMetric.descent), mPaint);
        canvas.drawText("Score", LEFT_BORDER, (float)(V_OFFSET + this.SCALE/6.0 - mFontMetric.descent), mPaint);

        float scoreOffset = mPaint.measureText("Highscore ");
        canvas.drawText(String.valueOf(this.mHighScore), LEFT_BORDER + scoreOffset, (float)(V_OFFSET + this.SCALE/12.0 - mFontMetric.descent), mPaint);
        canvas.drawText(String.valueOf(game.score), LEFT_BORDER + scoreOffset, (float)(V_OFFSET + this.SCALE/6.0 - mFontMetric.descent), mPaint);
    }

    private void draw(Cannon cannon, Canvas canvas) {
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Style.FILL);

        canvas.drawRect(
                H_OFFSET + (SCALE - CANNON_BASE_WIDTH) / 2.0f,
                BOTTOM_BORDER + SCALE / 6.0f - CANNON_BASE_HEIGHT,
                H_OFFSET + (SCALE - CANNON_BASE_WIDTH) / 2.0f + CANNON_BASE_WIDTH,
                BOTTOM_BORDER + SCALE / 6.0f,
                mPaint
        );

        canvas.drawCircle(H_OFFSET + SCALE / 2.0f,
                          BOTTOM_BORDER + SCALE / 6.0f - CANNON_BASE_HEIGHT,
                          CANNON_BASE_WIDTH / 2.0f,
                          mPaint);

        mPaint.setStyle(Style.FILL);
        mPaint.setStrokeWidth(CANNON_WIDTH);
        mPaint.setStrokeCap(Paint.Cap.BUTT);

        canvas.drawLine(
                H_OFFSET + SCALE / 2.0f,
                BOTTOM_BORDER + SCALE / 6.0f - CANNON_BASE_HEIGHT,
                H_OFFSET + SCALE / 2.0f + (float) Math.cos(cannon.getAngle()) * CANNON_LENGTH, // End of cannon
                BOTTOM_BORDER + SCALE / 6.0f - CANNON_BASE_HEIGHT - (float) Math.sin(cannon.getAngle()) * CANNON_LENGTH, // End of cannon
                mPaint
        );
    }

    private void draw(final Ball ball, Canvas canvas) {
        float x = LEFT_BORDER + ball.nx * SCALE;
        float y = BOTTOM_BORDER - ball.ny * SCALE;
        float r = ball.nr * SCALE;

        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Style.FILL);
        mPaint.setStrokeWidth(0);

        canvas.drawCircle(x, y, r, mPaint);

        mPaint.setAntiAlias(false);

        switch(ball.counter) {
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
                        x-r*0.1f,
                        y+r*0.15f,
                        x+r*0.5f,
                        y+r*0.3f,
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
                        y+r*0.15f,
                        x+r*0.1f,
                        y+r*0.3f,
                        mPaint);
            } break;
        }
        mPaint.setAntiAlias(true);
    }
}

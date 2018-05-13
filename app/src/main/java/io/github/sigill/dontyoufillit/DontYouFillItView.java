package io.github.sigill.dontyoufillit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class DontYouFillItView extends View {
    private DontYouFillItGame mGame = null;
    private Integer mHighScore = new Integer(0);

    private float SCALE, GAME_WIDTH, GAME_HEIGHT, V_OFFSET, H_OFFSET,
    BOTTOM_BORDER, TOP_BORDER, LEFT_BORDER, RIGHT_BORDER,
    CANNON_BASE_WIDTH, CANNON_BASE_HEIGHT, CANNON_LENGTH, CANNON_WIDTH;

    /** Timestamp of the last frame created */
    private long mLastFrameTimestamp = 0;

    /** Paint object */
    private final Paint mPaint = new Paint();
    private final PathEffect mDottedEffect = new DashPathEffect(new float[] {2, 2}, 0);
    private final Path mMainBorder = new Path(), mBottomBorder = new Path();
    private FontMetrics mFontMetric = null;

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
        setFocusable(true);

        reset();
    }

    private void reset() {
        resume();
    }

    public void resume() {
        mLastFrameTimestamp = System.currentTimeMillis();

        this.fpsCounter = 0;
        this.fpsCounterStart = mLastFrameTimestamp;

    }

    public void setGame(DontYouFillItGame game) {
        mGame = game;
    }

    public void setHighscore(Integer highscore) {
        mHighScore = highscore;
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
        mMainBorder.lineTo(this.RIGHT_BORDER-1, this.TOP_BORDER);
        mMainBorder.lineTo(this.RIGHT_BORDER-1, this.BOTTOM_BORDER);

        mBottomBorder.reset();
        mBottomBorder.moveTo(this.LEFT_BORDER, this.BOTTOM_BORDER);
        mBottomBorder.lineTo(this.RIGHT_BORDER-1, this.BOTTOM_BORDER);

        mFontMetric = mPaint.getFontMetrics();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeLayout();
    }

    public boolean onPauseButton(MotionEvent e) {
        return (e.getX() >= RIGHT_BORDER - SCALE / 6.0f)
                && (e.getX() <= RIGHT_BORDER)
                && (e.getY() >= TOP_BORDER - SCALE / 6.0f)
                && (e.getY() <= TOP_BORDER);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mGame == null)
            return;

        final long now = System.currentTimeMillis();
        final long elapsed = now - this.fpsCounterStart;
        this.fpsCounter++;

        if (elapsed >= 2000) {
            Log.v("FPS_COUNTER", (1000 * this.fpsCounter / (elapsed)) + "fps");
            this.fpsCounterStart = now;
            this.fpsCounter = 0;
        }

        mLastFrameTimestamp = now;

        if (mGame.state == DontYouFillItGame.State.GAMEOVER) {
            mPaint.setColor(Color.WHITE);
            mPaint.setAntiAlias(true);
            mPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Game Over", getWidth() / 2f, getHeight() / 2f, this.mPaint);
            mPaint.setTextAlign(Paint.Align.LEFT);

            return;
        }

        drawScene(canvas);

        draw(mGame.cannon, canvas);

        for (final Ball b : mGame.staticBalls)
            draw(b, canvas);

        if(mGame.currentBall != null)
            draw(mGame.currentBall, canvas);

        if (mGame.state == DontYouFillItGame.State.PAUSED) {
            mPaint.setColor(Color.BLACK);
            mPaint.setAlpha(220);
            canvas.drawRect(0, 0,
                            getWidth(), getHeight(), mPaint);

            mPaint.setColor(Color.WHITE);
            mPaint.setAlpha(255);
            mPaint.setAntiAlias(true);
            mPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Pause", getWidth() / 2f, getHeight() / 2f, this.mPaint);
            mPaint.setTextAlign(Paint.Align.LEFT);

        }
    }

    private void drawScene(Canvas canvas) {
        //mPaint.setColor(Color.DKGRAY);
        //canvas.drawRect(H_OFFSET, V_OFFSET,
        //                H_OFFSET + SCALE, BOTTOM_BORDER + SCALE/6.0f, mPaint);

        mPaint.setAntiAlias(false);

        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(0);

        canvas.drawPath(mMainBorder, mPaint);

        mPaint.setPathEffect(mDottedEffect);
        canvas.drawPath(mBottomBorder, mPaint); // Bottom
        mPaint.setPathEffect(null);

        mPaint.setStyle(Style.FILL);
        mPaint.setStrokeWidth(0);

        // Pause button
        canvas.drawRect(RIGHT_BORDER - SCALE / 6.0f * 0.9f,
                TOP_BORDER - SCALE / 6.0f * 0.9f,
                RIGHT_BORDER - SCALE / 6.0f * 0.6f,
                TOP_BORDER - SCALE / 6.0f * 0.1f,
                mPaint);

        canvas.drawRect(RIGHT_BORDER - SCALE / 6.0f * 0.4f,
                TOP_BORDER - SCALE / 6.0f * 0.9f,
                RIGHT_BORDER - SCALE / 6.0f * 0.1f,
                TOP_BORDER - SCALE / 6.0f * 0.1f,
                mPaint);

        mPaint.setColor(Color.WHITE);
        mPaint.setTextSize((float) (this.SCALE/12.0));

        mPaint.setAntiAlias(true);

        canvas.drawText("Highscore", LEFT_BORDER, (float)(V_OFFSET + this.SCALE/12.0 - mFontMetric.descent), mPaint);
        canvas.drawText("Score", LEFT_BORDER, (float)(V_OFFSET + this.SCALE/6.0 - mFontMetric.descent), mPaint);

        float scoreOffset = mPaint.measureText("Highscore ");
        canvas.drawText(String.valueOf(this.mHighScore), LEFT_BORDER + scoreOffset, (float)(V_OFFSET + this.SCALE/12.0 - mFontMetric.descent), mPaint);
        canvas.drawText(String.valueOf(mGame.score), LEFT_BORDER + scoreOffset, (float)(V_OFFSET + this.SCALE/6.0 - mFontMetric.descent), mPaint);
    }

    private void draw(Cannon cannon, Canvas canvas) {
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Style.FILL);
        mPaint.setAntiAlias(false);

        canvas.drawRect(
                H_OFFSET + (SCALE - CANNON_BASE_WIDTH) / 2.0f,
                BOTTOM_BORDER + SCALE / 6.0f - CANNON_BASE_HEIGHT,
                H_OFFSET + (SCALE - CANNON_BASE_WIDTH) / 2.0f + CANNON_BASE_WIDTH,
                BOTTOM_BORDER + SCALE / 6.0f,
                mPaint
        );

        mPaint.setAntiAlias(true);
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

        mPaint.setAntiAlias(true);

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
    }
}

package io.github.sigill.dontyoufillit;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends Activity implements Choreographer.FrameCallback, DontYouFillItGameListener {
    private static final String HIGHSCORE_PREF = "highscore";
    private DontYouFillItGame mGame = null;
    private DontYouFillItView dontYouFillItView;
    private long mLastTouchTimestamp = 0;
    private Integer mHighScore = 0;

    private SharedPreferences getPrefs() {
        // https://github.com/aosp-mirror/platform_frameworks_base/blob/3e5d8c4b9cbba8ecde7cd33dbc3bde8da8ba0c36/core/java/android/preference/PreferenceManager.java
        final String defaultSharedPreferenceName = this.getPackageName() + "_preferences";
        return getSharedPreferences(defaultSharedPreferenceName, Context.MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGame = new DontYouFillItGame();
        mGame.addListener(this);

        SharedPreferences prefs = getPrefs();
        this.mHighScore = prefs.getInt(HIGHSCORE_PREF, 0);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_game);
        dontYouFillItView = findViewById(R.id.dontYouFillItView);
        dontYouFillItView.setGame(mGame);
        dontYouFillItView.setHighscore(mHighScore);

        mGame.reset();
        dontYouFillItView.resume();

        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (super.onTouchEvent(event))
            return true;

        long previousTouchTimestamp = this.mLastTouchTimestamp;
        this.mLastTouchTimestamp = System.currentTimeMillis();

        if(previousTouchTimestamp > this.mLastTouchTimestamp - 500)
            return true;

//        Log.v("Touch", "X = " + e.getX() + ", Y = " + e.getY());

        if(mGame.state == DontYouFillItGame.State.GAMEOVER) {
            mGame.reset();
            dontYouFillItView.setHighscore(mHighScore);
            return true;
        }

        if(mGame.state == DontYouFillItGame.State.RUNNING && dontYouFillItView.onPauseButton(event)) {
            mGame.pause();
            return true;
        }

        if(mGame.state == DontYouFillItGame.State.RUNNING && mGame.currentBall == null) {
            mGame.fire();
            return true;
        }

        if(mGame.state == DontYouFillItGame.State.PAUSED) {
            mGame.resume();
            return true;
        }

        return true;
    }

    /*
     * Called when the activity is pushed from the top of the stack
     * of when it is about to be stopped.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("GAME_ACTIVITY", "Activity paused");
        if (mGame.state == DontYouFillItGame.State.RUNNING) {
            mGame.pause();
            dontYouFillItView.postInvalidate();
        }
    }

    @Override
    public void doFrame(long l) {
        if (mGame.state == DontYouFillItGame.State.RUNNING) {
            mGame.update(System.currentTimeMillis());
        }
        dontYouFillItView.postInvalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void onGameOver() {
        if(mGame.score > this.mHighScore) {
            this.mHighScore = mGame.score;

            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(HIGHSCORE_PREF, this.mHighScore);
            editor.commit();
        }
        dontYouFillItView.postInvalidate();
        Choreographer.getInstance().removeFrameCallback(this);
    }
}

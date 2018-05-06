package io.github.sigill.dontyoufillit;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends Activity {
    private DontYouFillItView dontYouFillItView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_game);
        dontYouFillItView = findViewById(R.id.dontYouFillItView);

        dontYouFillItView.resume();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    /*
     * Called when the activity comes at the top of the stack and
     * the user is about to be able to interact with it.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("GAME_ACTIVITY", "Activity resumed");
        dontYouFillItView.resume();
    }

    /*
     * Called when the activity is pushed from the top of the stack
     * of when it is about to be stopped.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("GAME_ACTIVITY", "Activity paused");
        dontYouFillItView.pause();
    }

    /*
     * Called when the application loses the focus.
     */
    /*
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("GAME_ACTIVITY", "Activity stopped");
        dontYouFillItView.pause();
    }
    */
}
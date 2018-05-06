package io.github.sigill.dontyoufillit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void launchGame(View v) {
        Intent i = new Intent(this, GameActivity.class);
        startActivity(i);
    }
}

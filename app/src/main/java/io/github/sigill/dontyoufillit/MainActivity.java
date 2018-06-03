package io.github.sigill.dontyoufillit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView versionView = findViewById(R.id.mainVersion);
        versionView.setText(BuildConfig.VERSION_NAME);
    }

    public void launchGame(View v) {
        Intent i = new Intent(this, GameActivity.class);
        startActivity(i);
    }

    public void openCredits(View v) {
        Intent i = new Intent(this, CreditsActivity.class);
        startActivity(i);
    }
}

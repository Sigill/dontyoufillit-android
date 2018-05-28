package io.github.sigill.dontyoufillit;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class CreditsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);
    }

    public void goBack(View v) {
        finish();
    }
}

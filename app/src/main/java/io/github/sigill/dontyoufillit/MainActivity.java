package io.github.sigill.dontyoufillit;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import static android.view.WindowManager.*;

public class MainActivity extends Activity {

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide the status bar.
        this.getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);

        mWebView = findViewById(R.id.activity_main_webview);

        mWebView.loadUrl("file:///android_asset/index.html");
    }
}

package io.github.sigill.dontyoufillit;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
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
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // This stuff is supposed to ensure the webview has no zoom, but it doesn't works.
        // https://developer.chrome.com/multidevice/webview/pixelperfect
        //mWebView.setPadding(0, 0, 0, 0);
        //mWebView.setInitialScale(1);
        //webSettings.setLoadWithOverviewMode(false);
        //webSettings.setUseWideViewPort(true);
        //webSettings.setBuiltInZoomControls(false);

        // Disable long click (could trigger text selection and display the copy/paste bar).
        mWebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        mWebView.setLongClickable(false);

        // No feedback on long touch.
        mWebView.setHapticFeedbackEnabled(false);

        // Starting with Android 4.4, the Chromium WebView takes care of localstorage persistence.
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            String databasePath = this.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
            webSettings.setDatabasePath(databasePath);
        }

        mWebView.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage cm) {
                if(BuildConfig.DEBUG) {
                    Log.d("DontYouFillIt", cm.message()
                            + " -- From line " + cm.lineNumber()
                            + " of " + cm.sourceId());
                }

                return true;
            }
        });

        mWebView.loadUrl("file:///android_asset/play.html");
    }
}

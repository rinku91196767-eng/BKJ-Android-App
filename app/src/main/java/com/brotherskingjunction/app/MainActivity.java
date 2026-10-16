package com.brotherskingjunction.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private static final String LIVE_URL = "https://brotherskingjunction.netlify.app/";
    private static final String LIVE_HOST = "brotherskingjunction.netlify.app";
    private static final String FALLBACK_URL = "file:///android_asset/offline.html";
    private WebView webView;
    private boolean fallbackShown = false;
    private String appStyleJs = "";
    private String headerFixJs = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(255, 107, 53));
        getWindow().setNavigationBarColor(Color.WHITE);

        appStyleJs = readAsset("app-style.js");
        headerFixJs = readAsset("header-fix.js");

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(255, 248, 245));
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && url.startsWith("https://" + LIVE_HOST)) {
                    injectUiFixes(view);
                    view.postDelayed(() -> injectUiFixes(view), 700);
                    view.postDelayed(() -> injectUiFixes(view), 1800);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (!fallbackShown && request.isForMainFrame()) {
                    Uri failed = request.getUrl();
                    if (failed != null && LIVE_HOST.equalsIgnoreCase(failed.getHost())) {
                        fallbackShown = true;
                        Toast.makeText(MainActivity.this, "Internet unavailable. Opening offline screen.", Toast.LENGTH_SHORT).show();
                        view.loadUrl(FALLBACK_URL);
                    }
                }
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(LIVE_URL);
        }
    }

    private void injectUiFixes(WebView view) {
        if (view == null) return;
        if (!appStyleJs.isEmpty()) view.evaluateJavascript(appStyleJs, null);
        if (!headerFixJs.isEmpty()) view.evaluateJavascript(headerFixJs, null);
    }

    private String readAsset(String name) {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(name), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
        } catch (Exception ignored) { }
        return out.toString();
    }

    private boolean handleUrl(String url) {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost();

        if ("file".equals(scheme)) return false;
        if (("http".equals(scheme) || "https".equals(scheme)) && host != null
                && (host.equalsIgnoreCase(LIVE_HOST) || host.endsWith("." + LIVE_HOST))) {
            return false;
        }

        try {
            Intent intent;
            if ("tel".equals(scheme)) {
                intent = new Intent(Intent.ACTION_DIAL, uri);
            } else if ("intent".equals(scheme)) {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } else {
                intent = new Intent(Intent.ACTION_VIEW, uri);
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Required app is not installed.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open this link.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
        }
        super.onDestroy();
    }
}

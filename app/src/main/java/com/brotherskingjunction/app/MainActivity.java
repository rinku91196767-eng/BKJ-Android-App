package com.brotherskingjunction.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private static final String LIVE_URL = "https://brotherskingjunction.netlify.app/";
    private static final String LIVE_HOST = "brotherskingjunction.netlify.app";
    private static final String FALLBACK_URL = "file:///android_asset/offline.html";

    private WebView webView;
    private FrameLayout root;
    private View menuTouchOverlay;
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

        root = new FrameLayout(this);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(255, 248, 245));
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        menuTouchOverlay = new View(this);
        menuTouchOverlay.setBackgroundColor(Color.TRANSPARENT);
        menuTouchOverlay.setClickable(true);
        menuTouchOverlay.setFocusable(true);
        menuTouchOverlay.setContentDescription("Open menu");
        menuTouchOverlay.setElevation(1000f);
        menuTouchOverlay.setVisibility(View.GONE);
        menuTouchOverlay.setOnClickListener(v -> toggleMenuFromNative());
        root.addView(menuTouchOverlay, new FrameLayout.LayoutParams(1, 1));

        setContentView(root);

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
                    positionNativeMenuOverlay();
                    view.postDelayed(() -> {
                        injectUiFixes(view);
                        positionNativeMenuOverlay();
                    }, 700);
                    view.postDelayed(() -> {
                        injectUiFixes(view);
                        positionNativeMenuOverlay();
                    }, 1800);
                } else {
                    menuTouchOverlay.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (!fallbackShown && request.isForMainFrame()) {
                    Uri failed = request.getUrl();
                    if (failed != null && LIVE_HOST.equalsIgnoreCase(failed.getHost())) {
                        fallbackShown = true;
                        menuTouchOverlay.setVisibility(View.GONE);
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

    private void positionNativeMenuOverlay() {
        if (webView == null || menuTouchOverlay == null) return;
        String js = "(function(){var e=document.getElementById('bkjMenuToggle');if(!e)return '';var r=e.getBoundingClientRect();return [r.left,r.top,r.width,r.height,(window.devicePixelRatio||1)].join(',');})()";
        webView.evaluateJavascript(js, value -> {
            try {
                if (value == null) {
                    menuTouchOverlay.setVisibility(View.GONE);
                    return;
                }
                String clean = value.replace("\\\"", "").replace("\"", "").trim();
                if (clean.isEmpty() || "null".equals(clean)) {
                    menuTouchOverlay.setVisibility(View.GONE);
                    return;
                }
                String[] p = clean.split(",");
                if (p.length < 5) {
                    menuTouchOverlay.setVisibility(View.GONE);
                    return;
                }
                float dpr = Float.parseFloat(p[4]);
                int rawLeft = Math.round(Float.parseFloat(p[0]) * dpr);
                int rawTop = Math.round(Float.parseFloat(p[1]) * dpr);
                int rawWidth = Math.round(Float.parseFloat(p[2]) * dpr);
                int rawHeight = Math.round(Float.parseFloat(p[3]) * dpr);

                float density = getResources().getDisplayMetrics().density;
                int minSize = Math.round(48f * density);
                int width = Math.max(rawWidth, minSize);
                int height = Math.max(rawHeight, minSize);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
                lp.leftMargin = Math.max(0, rawLeft - (width - rawWidth) / 2);
                lp.topMargin = Math.max(0, rawTop - (height - rawHeight) / 2);
                menuTouchOverlay.setLayoutParams(lp);
                menuTouchOverlay.bringToFront();
                menuTouchOverlay.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                menuTouchOverlay.setVisibility(View.GONE);
            }
        });
    }

    private void toggleMenuFromNative() {
        if (webView == null) return;
        String js = "(function(){var w=document.querySelector('.top .nav')||document.querySelector('.nav');var m=document.getElementById('bkjMainNav');var t=document.getElementById('bkjMenuToggle');if(!w||!m)return false;var open=!w.classList.contains('nav-open');w.classList.toggle('nav-open',open);if(t){t.setAttribute('aria-expanded',open?'true':'false');t.textContent=open?'×':'☰';}if(window.matchMedia('(max-width:1050px)').matches){m.style.setProperty('display',open?'grid':'none','important');if(open){m.style.setProperty('grid-template-columns','1fr 1fr','important');m.style.setProperty('width','100%','important');m.style.setProperty('order','4','important');m.style.setProperty('gap','6px','important');}}return open;})()";
        webView.evaluateJavascript(js, null);
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

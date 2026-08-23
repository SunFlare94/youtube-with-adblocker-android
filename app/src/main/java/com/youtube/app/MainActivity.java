package com.youtube.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.app.Activity;
import android.webkit.CookieManager;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;
    private SharedPreferences prefs;
    private Handler handler;
    private Runnable scriptInjector;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean isFullscreen = false;

    private static final String YOUTUBE_URL = "https://m.youtube.com/?theme=dark";

    private final Object toastLock = new Object();
    private long lastToastTime = 0;

    @JavascriptInterface
    public void toast(final String msg) {
        runOnUiThread(() -> {
            long now = System.currentTimeMillis();
            if (now - lastToastTime > 2000) {
                lastToastTime = now;
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static final Set<String> AD_DOMAINS = new HashSet<>();
    static {
        AD_DOMAINS.add("doubleclick.net");
        AD_DOMAINS.add("googleadservices.com");
        AD_DOMAINS.add("googlesyndication.com");
        AD_DOMAINS.add("pagead2.googlesyndication.com");
        AD_DOMAINS.add("securepubads.g.doubleclick.net");
        AD_DOMAINS.add("adservice.google.com");
        AD_DOMAINS.add("googleads.g.doubleclick.net");
        AD_DOMAINS.add("tpc.googlesyndication.com");
        AD_DOMAINS.add("www.googleadservices.com");
        AD_DOMAINS.add("ad.doubleclick.net");
        AD_DOMAINS.add("static.ads-twitter.com");
        AD_DOMAINS.add("ads.youtube.com");
        AD_DOMAINS.add("ade.googlesyndication.com");
    }

    private static final Set<String> AD_PATHS = new HashSet<>();
    static {
        AD_PATHS.add("/get_video_info");
        AD_PATHS.add("/youtubei/v1/player");
        AD_PATHS.add("ads");
        AD_PATHS.add("/adbreak");
    }

    private static final Set<String> TRACKING_DOMAINS = new HashSet<>();
    static {
        TRACKING_DOMAINS.add("google-analytics.com");
        TRACKING_DOMAINS.add("analytics.google.com");
        TRACKING_DOMAINS.add("stats.g.doubleclick.net");
    }

    private static final String AD_BLOCK_JS =
        "(function(){" +
        "  if(window.__sfAdBlock)return;" +
        "  window.__sfAdBlock=true;" +
        "" +
        "  var _fetch=window.fetch;" +
        "  window.fetch=function(){" +
        "    return _fetch.apply(this,arguments).then(function(resp){" +
        "      var ct=resp.headers.get('content-type')||'';" +
        "      if(ct.indexOf('json')===-1)return resp;" +
        "      return resp.clone().text().then(function(txt){" +
        "        try{" +
        "          var d=JSON.parse(txt);" +
        "          var changed=false;" +
        "          if(d.adPlacements){delete d.adPlacements;changed=true;}" +
        "          if(d.adSlots){delete d.adSlots;changed=true;}" +
        "          if(d.adClientParams){delete d.adClientParams;changed=true;}" +
        "          if(d.adBreakHeartbeatParams){delete d.adBreakHeartbeatParams;changed=true;}" +
        "          if(d.playerResponse){" +
        "            var p=d.playerResponse;" +
        "            if(p.adPlacements){delete p.adPlacements;changed=true;}" +
        "            if(p.adSlots){delete p.adSlots;changed=true;}" +
        "            if(p.adClientParams){delete p.adClientParams;changed=true;}" +
        "            if(p.adBreakHeartbeatParams){delete p.adBreakHeartbeatParams;changed=true;}" +
        "          }" +
        "          if(d.attestation&&d.attestation.playerAttestationRenderer){" +
        "            delete d.attestation;changed=true;" +
        "          }" +
        "          function cleanAds(node){" +
        "            if(!node||typeof node!=='object')return;" +
        "            if(Array.isArray(node)){node.forEach(cleanAds);return;}" +
        "            for(var k in node){" +
        "              if(k==='adSlotRenderer'||k==='promotedSparklesWebRenderer'||k==='adRenderer'){" +
        "                delete node[k];changed=true;" +
        "              }else if(typeof node[k]==='object'){cleanAds(node[k]);}" +
        "            }" +
        "          }" +
        "          cleanAds(d);" +
        "          if(changed){" +
        "            var body=JSON.stringify(d);" +
        "            return new Response(body,{status:resp.status,statusText:resp.statusText,headers:resp.headers});" +
        "          }" +
        "        }catch(e){}" +
        "        return resp;" +
        "      });" +
        "    });" +
        "  };" +
        "" +
        "  function killDOMAds(){" +
        "    try{" +
        "      var els=document.querySelectorAll(" +
        "        'ytd-display-ad-renderer,ytd-promoted-sparkles-web-renderer,ytd-ad-slot-renderer'," +
        "        'ytd-in-feed-ad-layout-renderer,ytd-banner-promo-renderer,ytd-statement-banner-renderer'," +
        "        'ytd-feed-nudge-renderer,ytd-promoted-video-renderer'" +
        "      );" +
        "      els.forEach(function(e){e.remove();});" +
        "    }catch(e){}" +
        "  }" +
        "  function skipVideoAds(){" +
        "    try{" +
        "      var btns=document.querySelectorAll(" +
        "        '.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button'," +
        "        'button.ytp-ad-skip-button-modern,.ytp-ad-skip-button-slot'," +
        "        '.ytp-ad-skip-button-container button'" +
        "      );" +
        "      btns.forEach(function(b){try{b.click();}catch(e){}});" +
        "      var ov=document.querySelector('.ytp-ad-overlay-container,.ytp-ad-text-overlay');" +
        "      if(ov)ov.remove();" +
        "      var vid=document.querySelector('.ad-showing video');" +
        "      if(vid&&vid.duration&&vid.duration>0)vid.currentTime=vid.duration;" +
        "    }catch(e){}" +
        "  }" +
        "" +
        "  killDOMAds();" +
        "  skipVideoAds();" +
        "  setInterval(killDOMAds,1000);" +
        "  setInterval(skipVideoAds,300);" +
        "  new MutationObserver(function(){killDOMAds();}).observe(document.body,{childList:true,subtree:true});" +
        "})();";

    private static final String DARK_MODE_CSS =
        "(function(){" +
        "if(window.__sfDarkMode)return;" +
        "window.__sfDarkMode=true;" +
        "var s=document.createElement(\"style\");" +
        "s.id=\"sf-dark-mode\";" +
        "s.textContent=" +
        "\"html,body{background:#121212!important;color:#e0e0e0!important;\"+" +
        "\"ytd-app{background:#121212!important;}\"+" +
        "\"ytd-browse{background:#121212!important;}\"+" +
        "\"ytd-section-list-renderer{background:#121212!important;}\"+" +
        "\"ytd-item-section-renderer{background:#121212!important;}\"+" +
        "\"ytd-rich-grid-renderer{background:#121212!important;}\"+" +
        "\"ytd-rich-item-renderer{background:#1e1e1e!important;}\"+" +
        "\"ytd-video-renderer{background:#1e1e1e!important;}\"+" +
        "\"#content{background:#121212!important;}\"+" +
        "\"ytd-searchbox-renderer input{background:#2a2a2a!important;color:#fff!important;}\"+" +
        "\"#chip-bar{background:#1e1e1e!important;}\"+" +
        "\"#chips-wrapper{background:#1e1e1e!important;}\"+" +
        "\"yt-chip-cloud-chip-renderer{background:#333!important;color:#fff!important;}\"+" +
        "\"ytd-guide-renderer{background:#1e1e1e!important;}\"+" +
        "\"#guide-content{background:#1e1e1e!important;}\"+" +
        "\"ytd-guide-entry-renderer{color:#fff!important;}\"+" +
        "\"#page-manager{background:#121212!important;}\"+" +
        "\"#below{background:#121212!important;}\"+" +
        "\"#primary{background:#121212!important;}\"+" +
        "\"#secondary{background:#121212!important;}\"+" +
        "\"ytd-comments-section-renderer{background:#121212!important;}\"+" +
        "\"ytd-comment-renderer{background:#1e1e1e!important;}\"+" +
        "\"#header{background:#1e1e1e!important;}\"+" +
        "\"#title{color:#fff!important;}\"+" +
        "\"#description{color:#ccc!important;}\"+" +
        "\"ytd-player{background:#000!important;}\";" +
        "document.head.appendChild(s);" +
        "})();";

    private static final String DARK_MODE_COOKIE =
        "PREF=tts=1&theme=dark&hl=en&gl=US; domain=.youtube.com; path=/; max-age=31536000";

    private static final String SPONSOR_BLOCK_JS =
        "(function(){" +
        "  if(window.__sfSbInit)return;" +
        "  window.__sfSbInit=true;" +
        "  var segs=[],curId='';" +
        "  function vid(){" +
        "    var u=location.href,m=u.match(/[?&]v=([a-zA-Z0-9_-]{11})/);" +
        "    return m?m[1]:(u.match(/youtu\\.be\\/([a-zA-Z0-9_-]{11})/)||[])[1]||'';" +
        "  }" +
        "  function fetchS(id){" +
        "    if(!id||id===curId)return;curId=id;segs=[];" +
        "    fetch('https://sponsor.ajay.app/api/skipSegments?videoID='+id)" +
        "      .then(function(r){return r.json();}).then(function(d){" +
        "        segs=d;" +
        "        if(d.length>0&&typeof Android!=='undefined')Android.toast('SponsorBlock: Found '+d.length+' segment(s)');" +
        "      }).catch(function(){});" +
        "  }" +
        "  function skip(){" +
        "    var p=document.getElementById('movie_player');if(!p||!segs.length)return;" +
        "    var v=p.querySelector('video');if(!v)return;var t=v.currentTime;" +
        "    for(var i=0;i<segs.length;i++){" +
        "      var s=segs[i],st=s.segment[0],en=s.segment[1],c=s.category;" +
        "      if((c==='sponsor'||c==='intro'||c==='outro'||c==='selfpromo'||c==='filler'||c==='interaction'||c==='preview'||c==='music_offtopic')&&t>=st&&t<en){" +
        "        v.currentTime=en;" +
        "        var m=Math.floor(st/60),sec=Math.floor(st%60),m2=Math.floor(en/60),sec2=Math.floor(en%60);" +
        "        var label=c.charAt(0).toUpperCase()+c.slice(1);" +
        "        if(typeof Android!=='undefined')Android.toast('SponsorBlock: Skipped '+label+' ('+m+':'+(sec<10?'0':'')+sec+' - '+m2+':'+(sec2<10?'0':'')+sec2+')');" +
        "        var n=document.createElement('div');n.id='sb-notice';" +
        "        n.style.cssText='position:fixed;bottom:80px;right:16px;background:rgba(0,0,0,0.85);color:#E3B779;padding:10px 16px;border-radius:8px;z-index:99999;font-size:13px;font-family:Arial;border-left:3px solid #E3B779;box-shadow:0 2px 8px rgba(0,0,0,0.4);';" +
        "        n.innerHTML='<b>SponsorBlock</b><br>Skipped '+label+' ('+m+':'+(sec<10?'0':'')+sec+' - '+m2+':'+(sec2<10?'0':'')+sec2+')';" +
        "        document.body.appendChild(n);setTimeout(function(){n.remove();},3000);" +
        "        break;" +
        "      }" +
        "    }" +
        "  }" +
        "  setInterval(function(){var v=vid();if(v)fetchS(v);skip();},500);" +
        "})();";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);

        setupWebView();

        webView.setBackgroundColor(0xFF121212);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setCookie("https://m.youtube.com", DARK_MODE_COOKIE);
        CookieManager.getInstance().setCookie("https://www.youtube.com", DARK_MODE_COOKIE);
        CookieManager.getInstance().setCookie("https://youtube.com", DARK_MODE_COOKIE);

        String loadUrl = YOUTUBE_URL;
        if (getIntent() != null && getIntent().getData() != null) {
            loadUrl = getIntent().getData().toString();
        }
        webView.loadUrl(loadUrl);
        startPeriodicInjection();
    }

    private void startPeriodicInjection() {
        scriptInjector = new Runnable() {
            @Override
            public void run() {
                if (webView != null) {
                    webView.evaluateJavascript(DARK_MODE_CSS, null);
                    if (prefs.getBoolean("ad_blocker_enabled", true)) {
                        webView.evaluateJavascript(AD_BLOCK_JS, null);
                    }
                    if (prefs.getBoolean("sponsor_block_enabled", true)) {
                        webView.evaluateJavascript(SPONSOR_BLOCK_JS, null);
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(scriptInjector);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        String userAgent = settings.getUserAgentString();
        userAgent = userAgent.replace("wv", "");
        settings.setUserAgentString(userAgent);

        webView.addJavascriptInterface(this, "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                view.evaluateJavascript(DARK_MODE_CSS, null);
                if (prefs.getBoolean("ad_blocker_enabled", true)) {
                    view.evaluateJavascript(AD_BLOCK_JS, null);
                }
                if (prefs.getBoolean("sponsor_block_enabled", true)) {
                    view.evaluateJavascript(SPONSOR_BLOCK_JS, null);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString().toLowerCase();

                if (prefs.getBoolean("ad_blocker_enabled", true)) {
                    for (String domain : AD_DOMAINS) {
                        if (url.contains(domain)) {
                            return new WebResourceResponse(
                                    "text/plain",
                                    "utf-8",
                                    new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))
                            );
                        }
                    }
                }

                if (prefs.getBoolean("block_tracking", true)) {
                    for (String domain : TRACKING_DOMAINS) {
                        if (url.contains(domain)) {
                            return new WebResourceResponse(
                                    "text/plain",
                                    "utf-8",
                                    new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))
                            );
                        }
                    }
                }

                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                result.confirm();
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                fullscreenContainer.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                isFullscreen = true;
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                fullscreenContainer.removeView(customView);
                fullscreenContainer.setVisibility(View.GONE);
                customView = null;
                webView.setVisibility(View.VISIBLE);
                isFullscreen = false;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getData() != null && webView != null) {
            webView.loadUrl(intent.getData().toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        startPeriodicInjection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(scriptInjector);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(scriptInjector);
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen && customView != null) {
            webView.getWebChromeClient().onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

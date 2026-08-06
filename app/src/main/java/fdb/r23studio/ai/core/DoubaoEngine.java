package fdb.r23studio.ai.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DoubaoEngine {

    public interface JsCallback {
        void onResult(String raw);
    }

    public static final String HOME_URL = "https://www.doubao.com/";

    private static final String BRIDGE_JS = "doubao_bridge.js";
    private static final String DOM_CONFIG_JSON = "doubao_dom_config.json";

    private WebView webView;
    private JsBridge jsBridge;

    private static final int REQUEST_CODE_FILE_CHOOSER = 1001;

    private ValueCallback<Uri[]> filePathCallback;
    private Activity activity;
    private Runnable pageLoadedListener;

    public void init(Activity activity, WebView wv, JsBridge bridge) {
        this.activity = activity;
        this.webView = wv;
        this.jsBridge = bridge;
        configureWebView();
    }

    /** 页面（含 reload）加载完成后回调，用于登录覆盖层自动打开扫码面板 */
    public void setOnPageLoadedListener(Runnable listener) {
        this.pageLoadedListener = listener;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(buildDesktopUserAgent(settings.getUserAgentString()));

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.addJavascriptInterface(jsBridge, "DoubaoNative");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    view.loadUrl(uri.toString());
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (isDoubaoHost(url)) {
                    injectBridge();
                    if (pageLoadedListener != null) {
                        view.post(pageLoadedListener);
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                startDownload(url, userAgent, contentDisposition, mimetype);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                DoubaoEngine.this.filePathCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    activity.startActivityForResult(intent, REQUEST_CODE_FILE_CHOOSER);
                } catch (Exception e) {
                    DoubaoEngine.this.filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(webView);
                resultMsg.sendToTarget();
                return true;
            }
        });
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            if (filePathCallback == null) {
                return false;
            }
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                } else if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return true;
        }
        return false;
    }

    public void loadHome() {
        if (webView != null) {
            webView.loadUrl(HOME_URL);
        }
    }

    public void reload() {
        if (webView != null) {
            webView.reload();
        }
    }

    private boolean isDoubaoHost(String url) {
        if (url == null) return false;
        try {
            String host = Uri.parse(url).getHost();
            return host != null && host.contains("doubao.com");
        } catch (Exception e) {
            return false;
        }
    }

    private void injectBridge() {
        String script = readAsset(BRIDGE_JS);
        String config = readAsset(DOM_CONFIG_JSON);
        if (script == null) return;
        String configEscaped = config != null ? config.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ") : "{}";
        String full = script + "\nDoubaoBridge.setConfig('" + configEscaped + "');\nDoubaoBridge.startObserving();";
        evaluateJs(full, null);
    }

    public void sendMessage(final String text, final String agentKey, final JsCallback callback) {
        String escaped = escapeJsString(text);
        String script = "DoubaoBridge.sendMessage('" + escaped + "', " + (agentKey == null ? "null" : "'" + agentKey + "'") + ");";
        evaluateJs(script, callback);
    }

    public void triggerLogin(JsCallback callback) {
        evaluateJs("DoubaoBridge.clickLogin();", callback);
    }

    public void triggerScanLogin(JsCallback callback) {
        evaluateJs("DoubaoBridge.triggerScanLogin();", callback);
    }

    public void isLoggedIn(JsCallback callback) {
        evaluateJs("DoubaoBridge.isLoggedIn();", callback);
    }

    public void startQrWatch() {
        evaluateJs("DoubaoBridge.startQrWatch();", null);
    }

    public void stopQrWatch() {
        evaluateJs("DoubaoBridge.stopQrWatch();", null);
    }

    public void getPageInfo(JsCallback callback) {
        evaluateJs("DoubaoBridge.getPageInfo();", callback);
    }

    private void evaluateJs(final String script, final JsCallback callback) {
        if (webView == null) {
            if (callback != null) callback.onResult(null);
            return;
        }
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript(script, new android.webkit.ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (callback != null) {
                            callback.onResult(value);
                        }
                    }
                });
            }
        });
    }

    private String escapeJsString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\r", " ").replace("\n", "\\n");
    }

    private String readAsset(String name) {
        try {
            InputStream is = webView.getContext().getAssets().open(name);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            is.close();
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private void startDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        try {
            String fileName = getFileName(contentDisposition, url);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setTitle(fileName);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadManager.enqueue(request);
        } catch (Exception ignored) {
        }
    }

    private String getFileName(String contentDisposition, String url) {
        String name = null;
        if (contentDisposition != null) {
            int index = contentDisposition.indexOf("filename=");
            if (index >= 0) {
                name = contentDisposition.substring(index + 9);
                name = name.replaceAll("[\"']", "").trim();
                if (name.endsWith(";")) {
                    name = name.substring(0, name.length() - 1).trim();
                }
            }
        }
        if (name == null || name.isEmpty()) {
            name = Uri.parse(url).getLastPathSegment();
        }
        if (name == null || name.isEmpty()) {
            name = "OABUOD-download-" + System.currentTimeMillis();
        }
        return name;
    }

    private String buildDesktopUserAgent(String defaultUA) {
        return defaultUA
                .replace("; wv", "")
                .replace("Version/4.0", "")
                .replace("Mobile Safari", "Safari");
    }
}

package oversecured.ovaa.vulns;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import oversecured.ovaa.utils.LoginUtils;

@SuppressLint("SetJavaScriptEnabled")
public class VulnerableWebViewActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        applyInsecureSettings();
        setAttackerCookies(getIntent().getStringExtra("cookie"));
        interceptRequests();
        handleFileChooser();
        grantGeolocation();

        String url = getIntent().getStringExtra("url");
        String html = getIntent().getStringExtra("html");
        if (url != null) {
            webView.loadUrl(url);
            webView.postUrl(url, new byte[0]);
        }
        if (html != null) {
            injectHtml(html);
        }
    }

    private void applyInsecureSettings() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowContentAccess(true);
        settings.setGeolocationEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        WebView.setWebContentsDebuggingEnabled(true);

        CookieManager.getInstance().setAcceptFileSchemeCookies(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new NativeBridge(), "native");
    }

    private void setAttackerCookies(String cookie) {
        if (cookie == null) {
            return;
        }
        CookieManager.getInstance().setCookie("https://oversecured.com", cookie);
    }

    private void injectHtml(String html) {
        webView.loadData("<html><body>" + html + "</body></html>", "text/html", "UTF-8");
        webView.loadDataWithBaseURL("https://oversecured.com", "<div>" + html + "</div>",
                "text/html", "UTF-8", null);
        webView.evaluateJavascript("document.title = '" + html + "'", null);
        webView.loadUrl("javascript:render('" + html + "')");
    }

    private void interceptRequests() {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("app".equals(uri.getScheme())) {
                    try {
                        return new WebResourceResponse("application/octet-stream", "UTF-8",
                                new FileInputStream(uri.getPath()));
                    } catch (FileNotFoundException ignored) {
                    }
                }
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri.getHost() != null && uri.getHost().contains("oversecured.com")) {
                    view.loadUrl(uri.toString());
                    return true;
                }
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, uri));
                return true;
            }
        });
    }

    private void handleFileChooser() {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                callback.onReceiveValue(new Uri[] {
                        Uri.fromFile(new File(getFilesDir(), "credentials.xml"))
                });
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback callback) {

                callback.invoke(origin, true, true);
            }
        });
    }

    private void grantGeolocation() {
        GeolocationPermissions.getInstance().allow("https://" + getIntent().getStringExtra("origin"));
    }

    private class NativeBridge {
        @JavascriptInterface
        public String readFile(String path) {
            try {
                return FileUtils.readFileToString(new File(path), "UTF-8");
            } catch (IOException e) {
                return null;
            }
        }

        @JavascriptInterface
        public String getPassword() {
            return LoginUtils.getInstance(VulnerableWebViewActivity.this).getLoginData().password;
        }
    }
}

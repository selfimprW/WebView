package com.selfimpr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Calendar;
/**
 * Description:
 * 通过WebView获取html内容：http://blog.csdn.net/z82367825/article/details/52187921
 * WebView加载https页面不能正常显示资源问题 http://blog.csdn.net/crazy_zihao/article/details/51557425
 * Created by Jiacheng on 2017/8/25.
 */
public class WVFragment extends Fragment implements View.OnClickListener {

    //private String loadUrl = "https://github.com/Tencent/VasSonic";
    private String loadUrl = "https://segmentfault.com/t/android";
    private ProgressBar progressBar;
    private WebView webView;
    private TextView wvTitle;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wv, container, false);
        rootView.findViewById(R.id.refresh).setOnClickListener(this);
        wvTitle = (TextView) rootView.findViewById(R.id.wv_title);
        initProgressBar(rootView);
        initWebView(rootView);
        return rootView;
    }

    private void initProgressBar(View rootView) {
        progressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
        progressBar.setMax(100);
        progressBar.setSecondaryProgress(R.color.colorAccent);
    }

    private void initWebView(View rootView) {
        ViewGroup layoutWeb = (ViewGroup) rootView.findViewById(R.id.layout_web);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        webView = new WebView(getActivity());
        webView.setLayoutParams(params);
        layoutWeb.addView(webView);

        //优化webview加载速度，WebView先不要自动加载图片，等页面finish后再发起图片加载
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.getSettings().setLoadsImagesAutomatically(true);
        } else {
            webView.getSettings().setLoadsImagesAutomatically(false);
        }
        webView.setWebChromeClient(getWebChromeClient());
        webView.setWebViewClient(getWebViewClient());
        initWVSettings();
        webView.loadUrl(loadUrl);
        webView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                return false;
            }
        });
        webView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                /**
                 * WebView.HitTestResult.UNKNOWN_TYPE 未知类型
                 * WebView.HitTestResult.PHONE_TYPE 电话类型
                 * WebView.HitTestResult.EMAIL_TYPE 电子邮件类型
                 * WebView.HitTestResult.GEO_TYPE 地图类型
                 * WebView.HitTestResult.SRC_ANCHOR_TYPE 超链接类型
                 * WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE 带有链接的图片类型
                 * WebView.HitTestResult.IMAGE_TYPE 单纯的图片类型
                 * WebView.HitTestResult.EDIT_TEXT_TYPE 选中的文字类型
                 */
                WebView.HitTestResult result = ((WebView) v).getHitTestResult();
                int type = result.getType(); //首先判断点击的类型
                String content = result.getExtra(); //获取具体信息，图片这里就是图片地址
                e("onLongClick--type:" + type + ",content:" + content);
                return false;
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWVSettings() {
        if (webView == null || webView.getSettings() == null) {
            return;
        }
        webView.getSettings().setJavaScriptEnabled(true);//启用js
        webView.addJavascriptInterface(new InJavaScriptLocalObj(), "java_obj");
        webView.getSettings().setBlockNetworkImage(false);//解决图片不显示
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); //设置缓存模式，由Cache-Control控制是否使用缓存
        webView.getSettings().setDomStorageEnabled(true); //开启数据缓存
        webView.getSettings().setAppCacheEnabled(true); //H5缓存，2.8版本开启
        webView.getSettings().setAppCacheMaxSize(1024 * 1024 * 5); //H5缓存大小5M
        webView.getSettings().setAppCachePath(getActivity().getApplicationContext().getDir("cache", Context.MODE_PRIVATE).getPath());

        //设置webView自动调整内部的内容大小
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        /**
         * 在5.0以下 Android 默认是 全允许
         * android WebView从lollipop（5.0）开始默认不允许混合模式，https当中不能加载http资源，需要开启 WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
         * MIXED_CONTENT_ALWAYS_ALLOW：允许从任何来源加载内容，即使起源是不安全的；
         * MIXED_CONTENT_NEVER_ALLOW：不允许Https加载Http的内容，即不允许从安全的起源去加载一个不安全的资源；
         * MIXED_CONTENT_COMPATIBILITY_MODE：当涉及到混合式内容时，WebView 会尝试去兼容最新Web浏览器的风格。
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.getSettings().setAllowFileAccess(false); //防止WebView File域同源策略漏洞攻击
        webView.getSettings().setSavePassword(false); //WebView明文存储密码漏洞修复

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                webView.removeJavascriptInterface("searchBoxJavaBridge_"); //防止远程代码执行
                webView.removeJavascriptInterface("accessibility"); //防止远程代码执行
                webView.removeJavascriptInterface("accessibilityTraversal"); //防止远程代码执行
            }
        } catch (NoSuchMethodError e) {
            e.printStackTrace();
        }

        //Logger.d("asdf", "userAgent:" + getSettings().getUserAgentString());
        //String us = webView.getSettings().getUserAgentString() + xxx(String类型); //指明页面在xxx中开启
        //webView.getSettings().setUserAgentString(us);
        e("userAgent:" + webView.getSettings().getUserAgentString());
    }

    private Calendar begin;

    private WebChromeClient getWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                e("onProgressChanged--newProgress:" + newProgress);
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                e("onReceivedTitle--title:" + title);
                if (wvTitle != null) {
                    wvTitle.setText(title);
                }
            }

            @Override
            public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
                super.onReceivedTouchIconUrl(view, url, precomposed);
                e("onReceivedTouchIconUrl--url:" + url);
            }
        };
    }

    public void e(String msg) {
        Log.e("wjc", msg);
    }

    private WebViewClient getWebViewClient() {
        return new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                e("onPageStarted--url:" + url);

                begin = Calendar.getInstance();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:window.java_obj.getSource('<head>'+" +
                        "document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                super.onPageFinished(view, url);
                e("onPageFinished--url:" + url);


                java.text.DecimalFormat df = new java.text.DecimalFormat("#.000");
                double between = (double) ((Calendar.getInstance()).getTimeInMillis() - begin.getTimeInMillis()) / 1000;
                //防止第一次就取得加载时间
                e("加载时间是：" + df.format(between) + "秒");
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                //todo 可参考   http://blog.csdn.net/crazy_zihao/article/details/51557425
                //super.onReceivedSslError(view, handler, error);
                // handler.cancel();// Android默认的处理方式
                handler.proceed();// 接受所有网站的证书
                // handleMessage(Message msg);// 进行其他处理
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }
        };
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.refresh && webView != null) {
            webView.reload();
        }
    }

    public void onBackPressed() {

    }

    private class InJavaScriptLocalObj {
        @JavascriptInterface
        public void getSource(String html) {
            Log.d("html=", html);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();

            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
    }


}

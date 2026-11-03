package com.coldcoffee.htvip;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * 模块配置页: WebView 加载 Liquid Glass 风格开关页,
 * 开关状态持久化到本 App 的 SharedPreferences, hook 侧通过
 * XSharedPreferences("htvip") 跨进程读取。
 */
public class ConfigActivity extends Activity {

    public static final String PREFS = "htvip";

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, Context.MODE_WORLD_READABLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0f1420"));

        WebView web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.setBackgroundColor(Color.parseColor("#0f1420"));
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "HTBridge");
        web.loadUrl("file:///android_asset/config/index.html");

        root.addView(web, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // 把当前开关值注入页面
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String url) {
                try {
                    JSONObject o = new JSONObject();
                    for (String k : new String[]{"vip", "badge", "voice", "revoke"}) {
                        o.put(k, prefs().getBoolean(k, true));
                    }
                    v.evaluateJavascript(
                            "window._prefs=" + o + ";loadPrefs();", null);
                } catch (Throwable ignored) {}
            }
        });
    }

    /** JS 桥: 保存开关 */
    private class Bridge {
        @JavascriptInterface
        public void save(String json) {
            try {
                JSONObject o = new JSONObject(json);
                SharedPreferences.Editor e = prefs().edit();
                for (String k : new String[]{"vip", "badge", "voice", "revoke"}) {
                    if (o.has(k)) e.putBoolean(k, o.getBoolean(k));
                }
                e.apply();
            } catch (Throwable t) {
                Toast.makeText(ConfigActivity.this, "保存失败: " + t, Toast.LENGTH_SHORT).show();
            }
        }
    }
}

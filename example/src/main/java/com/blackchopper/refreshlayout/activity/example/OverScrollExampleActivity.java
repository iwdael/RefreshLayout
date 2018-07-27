package com.blackchopper.refreshlayout.activity.example;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.blackchopper.refresh.core.api.Refresh;
import com.blackchopper.refreshlayout.R;
import com.blackchopper.refresh.core.listener.OnRefreshListener;

/**
 * 越界回弹使用演示
 */
public class OverScrollExampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example_overscroll);

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final WebView webView = (WebView) findViewById(R.id.webView);
        final Refresh refreshLayout = (Refresh) findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull Refresh refreshLayout) {
                webView.loadUrl("http://github.com");
            }
        });
        refreshLayout.autoRefresh();


        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            @Override
            public void onPageCommitVisible(WebView view, String url) {
                refreshLayout.finishRefresh();
            }
        });
//        TextView textView = (TextView) findViewById(R.id.textView);
//        textView.setMovementMethod(new ScrollingMovementMethod());
    }

}

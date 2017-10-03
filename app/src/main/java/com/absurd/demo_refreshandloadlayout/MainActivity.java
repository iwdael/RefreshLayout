package com.absurd.demo_refreshandloadlayout;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.absurd.refreshandloadlayout.OnRefreshAndLoadListener;
import com.absurd.refreshandloadlayout.RefreshAndLoadingLayout;


public class MainActivity extends Activity implements OnRefreshAndLoadListener {
    private RefreshAndLoadingLayout mSwipeLayout;
    private WebView mPage;
    private TextView mHint, mHinp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwipeLayout = (RefreshAndLoadingLayout) findViewById(R.id.swipe_container);
        mPage = (WebView) findViewById(R.id.page);
        mHint = (TextView) findViewById(R.id.hint);
        mHinp = (TextView) findViewById(R.id.hinp);
        WebSettings webSettings = mPage.getSettings();
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setJavaScriptEnabled(true);

        mPage.setWebViewClient(new WebViewClient());

        mPage.loadUrl("http://wap.qq.com");

        mSwipeLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh(boolean mCurrent) {
        mHint.setText("正在刷新，请等待");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 停止刷新
                mSwipeLayout.setRefreshing(false);
                mSwipeLayout.stopRefresh();
                mHint.setText("下拉刷新");
                mPage.loadUrl("http://wap.163.com");
            }
        }, 3000);
    }

    @Override
    public void onNormal(boolean mCurrent) {
        if (mCurrent)
            mHint.setText("下拉刷新");
        else
            mHinp.setText("下拉刷新");

    }

    @Override
    public void onLoose(boolean mCurrent) {
        if (mCurrent)
            mHint.setText("松手刷新");
        else
            mHinp.setText("松手刷新");
    }
}

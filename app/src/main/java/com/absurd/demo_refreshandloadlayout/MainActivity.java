package com.absurd.demo_refreshandloadlayout;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.absurd.refreshandloadlayout.OnRefreshAndLoadListener;
import com.absurd.refreshandloadlayout.RefreshAndLoadingLayout;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements OnRefreshAndLoadListener {
    private RefreshAndLoadingLayout mSwipeLayout;
    private TextView mHint, mHinp;
    private RecyclerView rc_view;
    RcAdapter adapter;
    List<String> data = new ArrayList<>();
    int page;
    boolean isRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rc_view = findViewById(R.id.rc_view);
        adapter = new RcAdapter();
        rc_view.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rc_view.setAdapter(adapter);
        mSwipeLayout = (RefreshAndLoadingLayout) findViewById(R.id.swipe_container);

        mHint = (TextView) findViewById(R.id.hint);
        mHinp = (TextView) findViewById(R.id.hinp);
        mSwipeLayout.setOnRefreshListener(this);

        data.clear();
        page = 1;

        for (int i = 0; i < 20; i++) {
            data.add("------" + (page * 10 + i) + "--------");
        }
        adapter.bindData(data);
    }

    @Override
    public void onRefresh(boolean mCurrent) {
        Log.v("TAG","------onRefresh----->>"+mCurrent);
        mHint.setText("正在刷新，请等待");
        mHinp.setText("正在刷新，请等待");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeLayout.stopRefresh();
                mHint.setText("下拉刷新");
                mHinp.setText("下拉刷新");
            }
        }, 500);
    }

    @Override
    public void onNormal(boolean mCurrent) {
        Log.v("TAG","------onNormal----->>"+mCurrent);
        if (mCurrent)
            mHint.setText("下拉刷新");
        else
            mHinp.setText("下拉刷新");

    }

    @Override
    public void onLoose(boolean mCurrent) {
        Log.v("TAG","------onLoose----->>"+mCurrent);
        if (mCurrent)
            mHint.setText("松手刷新");
        else
            mHinp.setText("松手刷新");
    }
}

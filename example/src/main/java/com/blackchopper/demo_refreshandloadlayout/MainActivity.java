package com.blackchopper.demo_refreshandloadlayout;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.blackchopper.refreshandloadlayout.RefreshAndLoadingLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * author  : Black Chopper
 * e-mail  : 4884280@qq.com
 * github  : http://github.com/BlackChopper
 * project :
 */
public class MainActivity extends Activity {
    private RefreshAndLoadingLayout mSwipeLayout;
    private RecyclerView rc_view;
    RcAdapter adapter;
    List<String> data = new ArrayList<>();
    RefreshAndLoadListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rc_view = findViewById(R.id.rc_view);
        adapter = new RcAdapter();
        rc_view.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rc_view.setAdapter(adapter);
        mSwipeLayout = (RefreshAndLoadingLayout) findViewById(R.id.swipe_container);
        listener = new RefreshAndLoadListener(this) {
            @Override
            protected void Refresh(int page) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        listener.stopRefresh(mSwipeLayout, new endListener() {
                            @Override
                            public void onEnd(boolean isHeader) {

                            }
                        });
                    }
                }, 2000);
            }
        };
        mSwipeLayout.setOnRefreshListener(listener);
        for (int i = 0; i < 15; i++) {
            data.add("------" + (1 * 10 + i) + "--------");
        }
        adapter.bindData(data);
    }


}

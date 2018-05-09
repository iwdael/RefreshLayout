package com.blackchopper.demo_refreshandloadlayout;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.blackchopper.refresh.core.RefreshLayout;
import com.blackchopper.refresh.core.api.Refresh;
import com.blackchopper.refresh.core.listener.OnLoadMoreListener;
import com.blackchopper.refresh.core.listener.OnRefreshListener;


/**
 * author  : Black Chopper
 * e-mail  : 4884280@qq.com
 * github  : http://github.com/BlackChopper
 * project :
 */
public class MainActivity extends Activity {
    RefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        refreshLayout = findViewById(R.id.refresh);
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull Refresh refreshLayout) {
                    refreshLayout.finishRefresh(2000);
            }
        });
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull Refresh refreshLayout) {
                refreshLayout.finishLoadMore(2000);
            }
        });
    }


}

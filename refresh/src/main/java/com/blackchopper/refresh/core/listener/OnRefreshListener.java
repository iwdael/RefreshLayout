package com.blackchopper.refresh.core.listener;

import android.support.annotation.NonNull;

import com.blackchopper.refresh.core.api.Refresh;

/**
 * 刷新监听器
 * Created by SCWANG on 2017/5/26.
 */

public interface OnRefreshListener {
    void onRefresh(@NonNull Refresh refreshLayout);
}

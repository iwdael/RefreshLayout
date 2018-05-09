package com.blackchopper.refresh.core.listener;


import android.support.annotation.NonNull;

import com.blackchopper.refresh.core.api.Refresh;
import com.blackchopper.refresh.core.constant.RefreshState;

/**
 * 刷新状态改变监听器
 * Created by SCWANG on 2017/5/26.
 */

public interface OnStateChangedListener {
    /**
     * 状态改变事件 {@link RefreshState}
     * @param refreshLayout Refresh
     * @param oldState 改变之前的状态
     * @param newState 改变之后的状态
     */
    void onStateChanged(@NonNull Refresh refreshLayout, @NonNull RefreshState oldState, @NonNull RefreshState newState);
}

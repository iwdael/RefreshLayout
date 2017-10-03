package com.absurd.refreshandloadlayout;

/**
 * Created by 段泽全 on 2017/10/2.
 * Github：https://github.com/mr-absurd
 * Emile:4884280@qq.com
 */

public interface OnRefreshAndLoadListener {
    void onNormal(boolean mCurrentIsHeader);

    void onLoose(boolean mCurrentIsHeader);

    void onRefresh(boolean mCurrentIsHeader);
}

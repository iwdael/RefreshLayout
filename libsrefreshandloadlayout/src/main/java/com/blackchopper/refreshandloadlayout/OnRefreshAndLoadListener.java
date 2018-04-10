package com.blackchopper.refreshandloadlayout;

/**
 * author  : Black Chopper
 * e-mail  : 4884280@qq.com
 * github  : http://github.com/BlackChopper
 * project :
 */
public interface OnRefreshAndLoadListener {
    void onNormal(boolean mCurrentIsHeader);

    void onLoose(boolean mCurrentIsHeader);

    void onRefresh(boolean mCurrentIsHeader);
}

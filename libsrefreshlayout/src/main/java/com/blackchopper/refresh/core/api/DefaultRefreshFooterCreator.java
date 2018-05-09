package com.blackchopper.refresh.core.api;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * author  : Black Chopper
 * e-mail  : 4884280@qq.com
 * github  : http://github.com/BlackChopper
 * project : RefreshLayout
 */
public interface DefaultRefreshFooterCreator {
    @NonNull
    RefreshFooter createRefreshFooter(@NonNull Context context, @NonNull Refresh layout);
}

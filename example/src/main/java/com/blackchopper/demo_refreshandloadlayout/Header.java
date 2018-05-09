package com.blackchopper.demo_refreshandloadlayout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.blackchopper.refresh.core.api.RefreshFooter;
import com.blackchopper.refresh.core.internal.InternalAbstract;

/**
 * author  : Black Chopper
 * e-mail  : 4884280@qq.com
 * github  : http://github.com/BlackChopper
 * project : RefreshLayout
 */
public class Header extends InternalAbstract implements RefreshFooter {
    protected Header(@NonNull View wrapper) {
        super(wrapper);
    }

    protected Header(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean setNoMoreData(boolean noMoreData) {
        return false;
    }
}

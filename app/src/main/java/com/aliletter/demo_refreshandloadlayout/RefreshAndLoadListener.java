package com.aliletter.demo_refreshandloadlayout;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aliletter.refreshandloadlayout.OnRefreshAndLoadListener;
import com.aliletter.refreshandloadlayout.RefreshAndLoadingLayout;


import java.util.List;

/**
 * Author: aliletter
 * Github: http://github.com/aliletter
 * Data: 2017/10/7.
 */

public abstract class RefreshAndLoadListener implements OnRefreshAndLoadListener {
    protected View view;
    public ImageView iv_refresh;
    public ImageView iv_load;
    public TextView tv_refresh;
    public TextView tv_load;
    public int mPage = 1;
    public boolean mIsRefresh = false;

    public RefreshAndLoadListener(Object object) {
        if (object instanceof Activity) {
            ViewGroup vg = (ViewGroup) ((Activity) object).getWindow().getDecorView();
            FrameLayout content = (FrameLayout) vg.findViewById(android.R.id.content);
            view = content.getChildAt(0);
        } else {
            this.view = (View) object;
        }
        iv_refresh = view.findViewById(R.id.iv_refresh);
        iv_load = view.findViewById(R.id.iv_load);
        tv_refresh = view.findViewById(R.id.tv_refresh);
        tv_load = view.findViewById(R.id.tv_load);

    }

    @Override
    public void onNormal(boolean b) {
        if (iv_refresh == null | iv_load == null) {
            return;
        }
        if (b) {
            Animation animation = iv_refresh.getAnimation();
            if (animation == null) {
                return;
            }
            animation.cancel();
            iv_refresh.clearAnimation();
            tv_refresh.setText(view.getContext().getString(R.string.refresh_normal));
        } else {
            Animation animation = iv_load.getAnimation();
            if (animation == null) {
                return;
            }
            animation.cancel();
            tv_load.setText(view.getContext().getString(R.string.load_normal));
            iv_load.clearAnimation();
        }
    }

    @Override
    public void onLoose(boolean b) {
        if (b) {
            tv_refresh.setText(view.getContext().getString(R.string.refresh_loose));
        } else {
            tv_load.setText(view.getContext().getString(R.string.load_loose));
        }
    }

    @Override
    public void onRefresh(boolean b) {
        if (b) mPage = 1;
        Refresh(mPage);
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.refreshandloalayout_rotate);
        if (b) {
            iv_refresh.startAnimation(animation);
            tv_refresh.setText("刷新中...");
        } else {
            iv_load.startAnimation(animation);
            tv_load.setText("加载中...");
        }
    }

    protected abstract void Refresh(int mPage);


    public void stopRefresh(final RefreshAndLoadingLayout layout, final endListener listener) {
        if (layout.isRefreshing()) {
            layout.stopRefresh();
        }


    }

    public void setPageByModule(List module) {
        if (module != null && module.size() >= 10) {
            mPage = mPage + 1;
        } else {
            mPage = -1;
        }
    }

    public void setPageByModule(List module, int limit) {

        if (module != null && module.size() >= limit) {
            mPage = mPage + 1;
        } else {
            mPage = -1;
        }

    }

    public interface endListener {
        void onEnd(boolean isHeader);
    }

    public void clearPage() {
        mPage = 1;
    }
}

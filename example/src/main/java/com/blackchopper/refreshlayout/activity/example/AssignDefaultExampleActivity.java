package com.blackchopper.refreshlayout.activity.example;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.blackchopper.refresh.core.api.Refresh;
import com.blackchopper.refreshlayout.R;
import com.blackchopper.refreshlayout.util.DynamicTimeFormat;
import com.blackchopper.refresh.core.RefreshLayout;
import com.blackchopper.refresh.core.api.DefaultRefreshFooterCreator;
import com.blackchopper.refresh.core.api.DefaultRefreshHeaderCreator;
import com.blackchopper.refresh.core.api.RefreshFooter;
import com.blackchopper.refresh.core.api.RefreshHeader;
import com.blackchopper.refresh.core.constant.RefreshState;
import com.blackchopper.refresh.core.constant.SpinnerStyle;
import com.blackchopper.refresh.core.footer.ClassicsFooter;
import com.blackchopper.refresh.core.header.ClassicsHeader;
import com.blackchopper.refresh.core.listener.SimpleMultiPurposeListener;

/**
 * 全局指定默认的Header和Footer
 */
public class AssignDefaultExampleActivity extends AppCompatActivity {

   private static boolean isFirstEnter = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        install();

        setContentView(R.layout.activity_example_assign_default);

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        /*
         * 以下代码仅仅为了演示效果而已，不是必须的
         * 关键代码在构造函数中
         */
        final Refresh refreshLayout = (Refresh) findViewById(R.id.refreshLayout);
        if (isFirstEnter) {
            isFirstEnter = false;
//            //触发上拉加载
//            refreshLayout.autoLoadMore();
            //通过多功能监听接口实现 在第一次加载完成之后 自动刷新
            refreshLayout.setOnMultiPurposeListener(new SimpleMultiPurposeListener(){
                @Override
                public void onStateChanged(@NonNull Refresh refreshLayout, @NonNull RefreshState oldState, @NonNull RefreshState newState) {
                    if (oldState == RefreshState.LoadFinish
                            && newState == RefreshState.None) {
                        refreshLayout.autoRefresh();
                        refreshLayout.setOnMultiPurposeListener(null);
                    }
                }
                @Override
                public void onLoadMore(@NonNull Refresh refreshLayout) {
                    refreshLayout.finishLoadMore(2000);
                }
                @Override
                public void onRefresh(@NonNull Refresh refreshLayout) {
                    refreshLayout.finishRefresh(3000);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        restore();
    }

    /*
     * 关键代码，需要在布局生成之前设置，建议代码放在 Application 中
     */
    private static void install() {
        //设置全局的Header构建器
        RefreshLayout.setDefaultRefreshHeaderCreator(new DefaultRefreshHeaderCreator() {
            @NonNull
            @Override
            public RefreshHeader createRefreshHeader(@NonNull Context context, @NonNull Refresh layout) {
                ClassicsHeader header = new ClassicsHeader(context).setSpinnerStyle(SpinnerStyle.FixedBehind);
                header.setPrimaryColorId(R.color.colorPrimary);
                header.setAccentColorId(android.R.color.white);
                return header;//指定为经典Header，默认是 贝塞尔雷达Header
            }
        });
        //设置全局的Footer构建器
        RefreshLayout.setDefaultRefreshFooterCreator(new DefaultRefreshFooterCreator() {
            @NonNull
            @Override
            public RefreshFooter createRefreshFooter(@NonNull Context context, @NonNull Refresh layout) {
                layout.setEnableLoadMoreWhenContentNotFull(true);//内容不满一页时候启用加载更多
                ClassicsFooter footer = new ClassicsFooter(context);
                footer.setBackgroundResource(android.R.color.white);
                footer.setSpinnerStyle(SpinnerStyle.Scale);//设置为拉伸模式
                return footer;//指定为经典Footer，默认是 BallPulseFooter
            }
        });
    }

    //还原默认 Header
    private static void restore() {
        RefreshLayout.setDefaultRefreshHeaderCreator(new DefaultRefreshHeaderCreator() {
            @NonNull
            @Override
            public RefreshHeader createRefreshHeader(@NonNull Context context, @NonNull Refresh layout) {
                layout.setPrimaryColorsId(R.color.colorPrimary, android.R.color.white);//全局设置主题颜色
                return new ClassicsHeader(context).setTimeFormat(new DynamicTimeFormat("更新于 %s"));
            }
        });
    }
}
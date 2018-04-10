package com.blackchopper.demo_refreshandloadlayout;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * author  : Black Chopper
 * e-mail  : 4884280@qq.com
 * github  : http://github.com/BlackChopper
 * project :
 */

public class RcViewHolder extends RecyclerView.ViewHolder {
    TextView tv;

    public RcViewHolder(View itemView) {
        super(itemView);
        tv = itemView.findViewById(R.id.tv_title);
    }

    public void bindData(String s) {
        tv.setText(s);
    }
}

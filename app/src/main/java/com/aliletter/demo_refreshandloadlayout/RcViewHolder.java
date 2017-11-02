package com.aliletter.demo_refreshandloadlayout;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Author: mr-absurd
 * Github: http://github.com/mr-absurd
 * Data: 2017/10/6.
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

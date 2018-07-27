package com.blackchopper.refreshlayout.fragment.index;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.blackchopper.refresh.core.api.Refresh;
import com.blackchopper.refreshlayout.R;
import com.blackchopper.refreshlayout.activity.style.BezierCircleStyleActivity;
import com.blackchopper.refreshlayout.activity.style.BezierRadarStyleActivity;
import com.blackchopper.refreshlayout.activity.style.ClassicsStyleActivity;
import com.blackchopper.refreshlayout.activity.style.DeliveryStyleActivity;
import com.blackchopper.refreshlayout.activity.style.DropBoxStyleActivity;
import com.blackchopper.refreshlayout.activity.style.FlyRefreshStyleActivity;
import com.blackchopper.refreshlayout.activity.style.FunGameBattleCityStyleActivity;
import com.blackchopper.refreshlayout.activity.style.FunGameHitBlockStyleActivity;
import com.blackchopper.refreshlayout.activity.style.MaterialStyleActivity;
import com.blackchopper.refreshlayout.activity.style.PhoenixStyleActivity;
import com.blackchopper.refreshlayout.activity.style.StoreHouseStyleActivity;
import com.blackchopper.refreshlayout.activity.style.TaurusStyleActivity;
import com.blackchopper.refreshlayout.activity.style.WaterDropStyleActivity;
import com.blackchopper.refreshlayout.activity.style.WaveSwipeStyleActivity;
import com.blackchopper.refreshlayout.adapter.BaseRecyclerAdapter;
import com.blackchopper.refreshlayout.adapter.SmartViewHolder;
import com.blackchopper.refreshlayout.util.StatusBarUtil;
import com.blackchopper.refresh.header.DropBoxHeader;
import com.blackchopper.refresh.header.FunGameHitBlockHeader;
import com.blackchopper.refresh.header.PhoenixHeader;
import com.blackchopper.refresh.core.api.RefreshHeader;
import com.blackchopper.refresh.core.footer.BallPulseFooter;
import com.blackchopper.refresh.core.header.ClassicsHeader;
import com.blackchopper.refresh.core.impl.RefreshHeaderWrapper;
import com.blackchopper.refresh.core.listener.OnRefreshListener;

import java.util.Arrays;

import static android.R.layout.simple_list_item_2;
import static android.support.v7.widget.DividerItemDecoration.VERTICAL;
import static com.blackchopper.refreshlayout.R.id.recyclerView;

/**
 * 风格展示
 * A simple {@link Fragment} subclass.
 */
public class RefreshStylesFragment extends Fragment implements AdapterView.OnItemClickListener {

    private enum Item {
        Hidden(R.string.title_activity_style_delivery,DeliveryStyleActivity.class),
        Delivery(R.string.title_activity_style_delivery,DeliveryStyleActivity.class),
        DropBox(R.string.title_activity_style_drop_box, DropBoxStyleActivity.class),
        WaveSwipe(R.string.title_activity_style_wave_swipe, WaveSwipeStyleActivity.class),
        FlyRefresh(R.string.title_activity_style_fly_refresh, FlyRefreshStyleActivity.class),
        WaterDrop(R.string.title_activity_style_water_drop, WaterDropStyleActivity.class),
        Material(R.string.title_activity_style_material, MaterialStyleActivity.class),
        Phoenix(R.string.title_activity_style_phoenix, PhoenixStyleActivity.class),
        Taurus(R.string.title_activity_style_taurus, TaurusStyleActivity.class),
        Bezier(R.string.title_activity_style_bezier, BezierRadarStyleActivity.class),
        Circle(R.string.title_activity_style_circle, BezierCircleStyleActivity.class),
        FunGameHitBlock(R.string.title_activity_style_hit_block, FunGameHitBlockStyleActivity.class),
        FunGameBattleCity(R.string.title_activity_style_battle_city, FunGameBattleCityStyleActivity.class),
        StoreHouse(R.string.title_activity_style_storehouse, StoreHouseStyleActivity.class),
        Classics(R.string.title_activity_style_classics, ClassicsStyleActivity.class),
        ;
        public int nameId;
        public Class<?> clazz;
        Item(@StringRes int nameId, Class<?> clazz) {
            this.nameId = nameId;
            this.clazz = clazz;
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_refresh_styles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        StatusBarUtil.setPaddingSmart(getContext(), root.findViewById(R.id.toolbar));

        View view = root.findViewById(recyclerView);
        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), VERTICAL));
            recyclerView.setAdapter(new BaseRecyclerAdapter<Item>(Arrays.asList(Item.values()), simple_list_item_2,this) {
                @Override
                public SmartViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    SmartViewHolder holder = super.onCreateViewHolder(parent, viewType);
                    if (viewType == 0) {
                        holder.itemView.setVisibility(View.GONE);
                        holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0,0));
                    }
                    return holder;
                }

                @Override
                public int getViewTypeCount() {
                    return 2;
                }

                @Override
                public int getItemViewType(int position) {
                    return position == 0 ? 0 : 1;
                }

                @Override
                protected void onBindViewHolder(SmartViewHolder holder, Item model, int position) {
                    holder.text(android.R.id.text1, model.name());
                    holder.text(android.R.id.text2, model.nameId);
                    holder.textColorId(android.R.id.text2, R.color.colorTextAssistant);
                }
            });
        }


        Refresh refreshLayout = (Refresh) root.findViewById(R.id.refreshLayout);
        if (refreshLayout != null) {
            refreshLayout.setOnRefreshListener(new OnRefreshListener() {
                @Override
                public void onRefresh(@NonNull final Refresh refreshLayout) {
                    refreshLayout.finishRefresh(3000);
                    refreshLayout.getLayout().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            RefreshHeader refreshHeader = refreshLayout.getRefreshHeader();
                            if (refreshHeader instanceof RefreshHeaderWrapper) {
                                refreshLayout.setRefreshHeader(new PhoenixHeader(getContext()));
                            } else if (refreshHeader instanceof PhoenixHeader) {
                                refreshLayout.setRefreshHeader(new DropBoxHeader(getContext()));
                            } else if (refreshHeader instanceof DropBoxHeader) {
                                refreshLayout.setRefreshHeader(new FunGameHitBlockHeader(getContext()));
                            } else if (refreshHeader instanceof FunGameHitBlockHeader) {
                                refreshLayout.setRefreshHeader(new ClassicsHeader(getContext()));
                            } else {
                                refreshLayout.setRefreshHeader(new RefreshHeaderWrapper(new BallPulseFooter(getContext())));
                            }
                            refreshLayout.setPrimaryColorsId(R.color.colorPrimary, android.R.color.white);
                        }
                    },4000);
                }
            });
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(new Intent(getContext(), Item.values()[position].clazz));
    }
}

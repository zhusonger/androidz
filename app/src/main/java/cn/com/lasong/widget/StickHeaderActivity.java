package cn.com.lasong.widget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.ArrayList;
import java.util.List;

import cn.com.lasong.R;
import cn.com.lasong.app.AppBaseActivity;
import cn.com.lasong.databinding.ActivityStickHeaderBinding;
import cn.com.lasong.utils.TN;
import cn.com.lasong.widget.adapterview.itemdecoration.StickHeaderProvider;
import cn.com.lasong.widget.adapterview.itemdecoration.StickyHeaderDecoration;
import cn.com.lasong.widget.adapterview.adapter.OnItemClickListener;
import cn.com.lasong.widget.adapterview.adapter.ZRecyclerViewAdapter;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/5/10
 * Description:
 */
public class StickHeaderActivity extends AppBaseActivity implements StickHeaderProvider{

    ActivityStickHeaderBinding binding;
    ZRecyclerViewAdapter<String> adapter;
    List<String> data = new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStickHeaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        for (int i = 0; i < 100; i++) {
            if (i % 10 == 0) {
                data.add("Group"+i);
            } else {
                data.add("Text"+i);
            }
        }
        adapter = new ZRecyclerViewAdapter<String>(data, R.layout.item_sticker_header, new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                TN.show(data.get(position) +":show");
            }
        }) {
            @Override
            public void bind(AdapterViewHolder holder, String item, int position) {
                boolean group = position % 10 == 0;
                holder.setVisible(group, R.id.tv_header);
                holder.setVisible(!group, R.id.tv_text);
                holder.setText(R.id.tv_header, item);
                holder.setText(R.id.tv_text, item);
                holder.setOnClickListener(R.id.tv_text);
            }

            final StickyHeaderDecoration stickyHeaderDecoration = new StickyHeaderDecoration();
            @Override
            public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
                super.onAttachedToRecyclerView(recyclerView);
                stickyHeaderDecoration.attachRecyclerView(recyclerView, StickHeaderActivity.this);
            }

            @Override
            public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
                super.onDetachedFromRecyclerView(recyclerView);
                stickyHeaderDecoration.detachRecyclerView(recyclerView);
            }
        };
        StickyHeaderDecoration decoration = new StickyHeaderDecoration();
        binding.rv.addItemDecoration(decoration);
        binding.rv.setLayoutManager(new LinearLayoutManager(this));
        binding.rv.setAdapter(adapter);
    }

    @Override
    public RecyclerView.ViewHolder createOrUpdateHeader(RecyclerView.ViewHolder cache, LayoutInflater inflater, RecyclerView parent, int position) {
        if (!isStickHeader(position)) {
            return null;
        }
        ViewHolder holder = cache;
        if (null == holder) {
            View v = inflater.inflate(R.layout.item_sticker_header, parent, false);
            holder = new ZRecyclerViewAdapter.AdapterViewHolder(v, null, null);
        }

        adapter.bind((ZRecyclerViewAdapter.AdapterViewHolder) holder, data.get(position), position);
        return holder;
    }

    @Override
    public boolean isStickHeader(int position) {
        return position % 10 == 0;
    }
}

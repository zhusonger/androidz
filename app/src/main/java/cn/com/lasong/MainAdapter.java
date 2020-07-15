package cn.com.lasong;

import android.app.Activity;
import android.content.Intent;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cn.com.lasong.base.AppManager;
import cn.com.lasong.lyric.LyricActivity;
import cn.com.lasong.move.MoveActivity;
import cn.com.lasong.resample.ResampleActivity;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020-03-04
 * Description:
 */
public class MainAdapter extends RecyclerView.Adapter<MainItemHolder> implements View.OnClickListener {

    private SparseArray<String> mKeys = new SparseArray<>();
    private SparseIntArray mIcons = new SparseIntArray();
    private SparseArray<Class<?>> mValues = new SparseArray<>();

    public MainAdapter() {
        mKeys.append(0, "歌词控件");
        mIcons.append(0, R.drawable.ic_lyric);
        mValues.append(0, LyricActivity.class);

        mKeys.append(1, "移动控件");
        mIcons.append(1, R.drawable.ic_move);
        mValues.append(1, MoveActivity.class);

        mKeys.append(2, "重采样");
        mIcons.append(2, R.drawable.ic_move);
        mValues.append(2, ResampleActivity.class);
    }

    @NonNull
    @Override
    public MainItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =  LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main, parent, false);
        return new MainItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainItemHolder holder, int position) {
        holder.mTvName.setText(mKeys.get(position));
        holder.mIvIcon.setImageResource(mIcons.get(position));
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(this);

    }

    @Override
    public int getItemCount() {
        return mKeys.size();
    }

    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if (null == tag) {
            return;
        }
        int position = (int) tag;
        Class<?> cls = mValues.get(position);
        Activity activity = AppManager.getInstance().current();
        if (null != activity) {
            Intent intent = new Intent(activity, cls);
            activity.startActivity(intent);
        }
    }
}

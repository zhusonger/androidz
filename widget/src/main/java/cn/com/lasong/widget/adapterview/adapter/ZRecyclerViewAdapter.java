package cn.com.lasong.widget.adapterview.adapter;

import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/1/29
 * Description: 
 */
public abstract class ZRecyclerViewAdapter<T> extends RecyclerView.Adapter<ZRecyclerViewAdapter.AdapterViewHolder> {
    private List<T> data;
    private int itemLayoutId;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public ZRecyclerViewAdapter(List<T> data, @LayoutRes int itemLayoutId) {
        this.data = data;
        this.itemLayoutId = itemLayoutId;
    }

    public ZRecyclerViewAdapter(List<T> data, @LayoutRes int itemLayoutId, OnItemClickListener clickListener) {
        this.data = data;
        this.itemLayoutId = itemLayoutId;
        this.clickListener = clickListener;
    }


    public ZRecyclerViewAdapter(List<T> data, @LayoutRes int itemLayoutId, OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
        this.data = data;
        this.itemLayoutId = itemLayoutId;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    @Override
    public AdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(itemLayoutId, parent, false);
        return new AdapterViewHolder(v, clickListener, longClickListener);
    }

    @Override
    public void onBindViewHolder(AdapterViewHolder holder, int position) {
        T item = data.get(position);
        bind(holder, item, position);
    }

    /**
     * @param holder
     * @param item
     * @param position
     */
    public abstract void bind(AdapterViewHolder holder, T item, int position);


    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    public static class AdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, Drawable.Callback {
        private SparseArray<View> views;
        private OnItemClickListener clickListener;
        private OnItemLongClickListener longClickListener;

        private SparseArray<TextView> mGifViews;

        @SuppressWarnings("unused")
        public AdapterViewHolder(@NonNull View itemView) {
            this(itemView, null, null);
        }

        public AdapterViewHolder(View itemView, OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
            super(itemView);
            this.clickListener = clickListener;
            this.longClickListener = longClickListener;
            views = new SparseArray<>();
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public <V extends View> V getView(int viewId) {
            View view = views.get(viewId);
            if (view == null) {
                view = itemView.findViewById(viewId);
                views.put(viewId, view);
            }
            //noinspection unchecked
            return (V) view;
        }

        /**
         * 设置文字内容
         */
        public AdapterViewHolder setText(int viewId, CharSequence text) {
            TextView view = getView(viewId);
            view.setText(text);
            return this;
        }

        public AdapterViewHolder setTextColor(int viewId, int color) {
            TextView view = getView(viewId);
            view.setTextColor(color);
            return this;
        }

        public AdapterViewHolder setTextChecked(int viewId, boolean checked) {
            CheckedTextView textView = getView(viewId);
            textView.setChecked(checked);
            return this;
        }
        public AdapterViewHolder setText(int viewId, @StringRes int textRes) {
            TextView view = getView(viewId);
            view.setText(textRes);
            return this;
        }

        public AdapterViewHolder setImageResource(int viewId, @DrawableRes int drawableRes) {
            ImageView view = getView(viewId);
            view.setImageResource(drawableRes);
            return this;
        }

        public AdapterViewHolder setGifTextView(int viewId) {
            View view = getView(viewId);
            if (!(view instanceof TextView)) {
                return this;
            }
            if (null == mGifViews) {
                mGifViews = new SparseArray<>();
            }
            mGifViews.put(viewId, (TextView) view);
            return this;
        }

        /**
         * 给Button按钮设置背景图片
         */
        public AdapterViewHolder setBtnBg(int id, Drawable drawable) {
            Button view = getView(id);
            view.setBackground(drawable);
            return this;
        }

        public AdapterViewHolder setVisible(boolean visible, int... ids) {
            for (int id : ids) {
                View v = getView(id);
                v.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
            return this;
        }

        /**
         * 将viewholder中的某些view的点击事件交给adapter统一处理
         */
        public AdapterViewHolder setOnClickListener(int... ids) {
            for (int i : ids) {
                View v = getView(i);
                v.setOnClickListener(this);
            }
            return this;
        }

        public AdapterViewHolder setChecked(int viewid, boolean checked) {
            CheckBox checkBox = getView(viewid);
            checkBox.setChecked(checked);
            return this;
        }

        public AdapterViewHolder setSelected(int viewid, boolean checked) {
            ImageView iv = getView(viewid);
            iv.setSelected(checked);
            return this;
        }

        /**
         * 将viewholder中的某些view的长按事件交给adapter统一处理
         */
        public AdapterViewHolder setOnLongClickListener(int... ids) {
            for (int i : ids) {
                View v = getView(i);
                if (!v.isLongClickable()) {
                    v.setLongClickable(true);
                }
                v.setOnLongClickListener(this);
            }
            return this;
        }


        @Override
        public void onClick(View v) {
            if (clickListener != null) {
                clickListener.onItemClick(v, getLayoutPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(v, getLayoutPosition());
            }
            //返回true，不再执行onclick事件；返回false，继续执行onclock事件
            return true;
        }

        @Override
        public void invalidateDrawable(@NonNull Drawable who) {
            int size = mGifViews.size();
            for (int i = 0; i < size; i++) {
                TextView tv = mGifViews.valueAt(i);
                if(null != tv && ViewCompat.isAttachedToWindow(tv)) {
                    tv.invalidate();
                }
            }

        }

        @Override
        public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
            int size = mGifViews.size();
            for (int i = 0; i < size; i++) {
                TextView tv = mGifViews.valueAt(i);
                if(null != tv) {
                    tv.postDelayed(what, when);
                }
            }
        }

        @Override
        public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
            int size = mGifViews.size();
            for (int i = 0; i < size; i++) {
                TextView tv = mGifViews.valueAt(i);
                if(null != tv) {
                    tv.removeCallbacks(what);
                }
            }
        }
    }
}

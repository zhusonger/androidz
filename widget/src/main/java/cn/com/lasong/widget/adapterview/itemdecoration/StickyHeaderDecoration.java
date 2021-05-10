package cn.com.lasong.widget.adapterview.itemdecoration;

import android.graphics.Canvas;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/5/7
 * Description: 吸顶分割线
 */
public class StickyHeaderDecoration extends RecyclerView.ItemDecoration implements RecyclerView.OnItemTouchListener {

    private int stickyHeaderHeight;
    private StickHeaderProvider headerProvider;
    SparseArray<ViewHolder> cacheHeaders = new SparseArray<>();

    /**
     * 绑定到RecyclerView
     * @param recyclerView
     * @param provider
     */
    public void attachRecyclerView(RecyclerView recyclerView, StickHeaderProvider provider) {
        if (null == recyclerView) {
            return;
        }
        headerProvider = provider;
        cacheHeaders.clear();
        recyclerView.addItemDecoration(this);
        recyclerView.addOnItemTouchListener(this);
    }

    /**
     * 从RecyclerView移除
     * @param recyclerView
     */
    public void detachRecyclerView(RecyclerView recyclerView) {
        headerProvider = null;
        cacheHeaders.clear();
        if (null == recyclerView) {
            return;
        }
        recyclerView.removeItemDecoration(this);
        recyclerView.removeOnItemTouchListener(this);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        if (e.getY() <= stickyHeaderHeight) {
            // Handle the clicks on the header here ...
            return true;
        }
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }


    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);
        if (null == headerProvider) {
            return;
        }

        // 1. 获取第一个可视视图的适配器位置
        View topChild = parent.getChildAt(0);
        if (topChild == null) {
            return;
        }
        int topChildPosition = parent.getChildAdapterPosition(topChild);
        if (topChildPosition == RecyclerView.NO_POSITION) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        // 2. 获取第一个可视视图的分组Holder
        ViewHolder header = null;
        for (int i = topChildPosition; i >= 0; i--) {
            // 忽略非吸顶头部
            if (!headerProvider.isStickHeader(i)) {
                continue;
            }
            // 获取头布局holder, 先取缓存, 没有再创建, 更新也在该方法内
            ViewHolder holder = headerProvider.createOrUpdateHeader(cacheHeaders.get(i), inflater, parent, i);
            // 每次都计算下宽高, 可能会调整内容
            if (null != holder) {
                header = holder;
                cacheHeaders.put(i, holder);
                View view = holder.itemView;
                // Specs for parent (RecyclerView)
                int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);

                // Specs for children (headers)
                int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                        parent.getPaddingLeft() + parent.getPaddingRight(), view.getLayoutParams().width);
                int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                        parent.getPaddingTop() + parent.getPaddingBottom(), view.getLayoutParams().height);

                view.measure(childWidthSpec, childHeightSpec);
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                stickyHeaderHeight = header.itemView.getMeasuredHeight();
                break;
            }
        }
        if (null == header) {
            return;
        }
        // 3. 寻找吸顶头与之相接的view, 用来判断是否需要移动吸顶
        View headerView = header.itemView;
        final int contactPoint = headerView.getBottom();
        View contactChild = null;
        // getChildCount 只会返回显示中的数量
        for (int i = 0, layoutCount = parent.getChildCount(); i < layoutCount; i++) {
            View child = parent.getChildAt(i);
            if (child.getBottom() > contactPoint) {
                if (child.getTop() <= contactPoint) {
                    // This child overlaps the contactPoint
                    contactChild = child;
                    break;
                }
            }
        }
        if (null == contactChild) {
            return;
        }

        // 4. 获取连接view的适配器位置, 判断是否也是吸顶头部
        // 如果是就进行移动, 否则画出吸顶头部
        int contactPosition = parent.getChildAdapterPosition(contactChild);
        // 新的头已经到来, 移走之前的头
        if (headerProvider.isStickHeader(contactPosition)) {
            canvas.save();
            canvas.translate(0, contactChild.getTop() - contactChild.getHeight());
            headerView.draw(canvas);
            canvas.restore();
            return;
        }

        // 画当前分组的头 显示在最上层位置, 吸顶效果
        canvas.save();
        canvas.translate(0, 0);
        headerView.draw(canvas);
        canvas.restore();
    }
}

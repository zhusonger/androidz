package cn.com.lasong.widget.adapterview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Author: zhusong
 * Email: song.zhu@kascend.com
 * Date: 2019/05/08
 * Description:  嵌套的内部RecyclerView，屏蔽父类的滑动事件
 */
public class NestedRecyclerView extends RecyclerView {
    public NestedRecyclerView(@NonNull Context context) {
        super(context);
    }

    public NestedRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NestedRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return super.onTouchEvent(e);
    }
}

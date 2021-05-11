package cn.com.lasong.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import cn.com.lasong.widget.any.AnyIndexListener;
import cn.com.lasong.widget.any.AnyIndexProvider;
import cn.com.lasong.widget.utils.ViewHelper;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/5/11
 * Description: 任意控件形式的索引控件
 */
public class AnyIndexView extends LinearLayout {
    public AnyIndexView(Context context) {
        this(context, null);
    }

    public AnyIndexView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnyIndexView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        if (isInEditMode()) {
            String[] array = new String[] {"*", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
                    "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W",
                    "X", "Y", "Z"};
            for (int i = 0; i < array.length; i++) {
                TextView textView = new TextView(context);
                textView.setText(array[i]);
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                addView(textView);
            }
        }
    }

    private AnyIndexProvider provider;
    public void setProvider(AnyIndexProvider provider) {
        this.provider = provider;
    }


    private AnyIndexListener listener;
    public void setListener(AnyIndexListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean ret = super.dispatchTouchEvent(ev);
        // 拦截索引的触摸事件
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean ret = super.onInterceptTouchEvent(ev);
        return true;
    }

    private static final int FLAG_PRE_PRESSED = 0x2;
    private static final int FLAG_PRESSED  = 0x1;
    public int mPrivateFlags;

    // 延迟更新点击状态
    private Runnable mPendingCheckForTap = null;

    private float mLastX;
    private float mLastY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);
        final float x = event.getX();
        final float y = event.getY();

        mLastX = x;
        mLastY = y;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(ViewHelper.pointInView(this, x, y)) {
                    // 设置为预按压
                    mPrivateFlags |= FLAG_PRE_PRESSED;
                    if (mPendingCheckForTap == null) {
                        mPendingCheckForTap = () -> {
                            mPrivateFlags &= ~FLAG_PRE_PRESSED;
                            mPrivateFlags |= FLAG_PRESSED;
                            touchIndex(mLastX, mLastY);
                        };
                    }
                    // 长按才开始进入按下状态
                    postDelayed(mPendingCheckForTap, ViewConfiguration.getLongPressTimeout());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // 在移动范围内, 并且已经是按下状态了, 才进行按下事件处理
                if (ViewHelper.pointInView(this, x, y)
                        && (mPrivateFlags & FLAG_PRESSED) != 0){
                    touchIndex(x, y);
                    mPrivateFlags &= ~FLAG_PRESSED;
                    mPrivateFlags |= FLAG_PRE_PRESSED;
                    postDelayed(mPendingCheckForTap, ViewConfiguration.getPressedStateDuration());
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // 还是预按下状态, 松下如果是抬起就直接执行按下事件
                boolean prepressed = (mPrivateFlags & FLAG_PRE_PRESSED) != 0;
                if (prepressed && event.getAction() == MotionEvent.ACTION_UP) {
                    removeCallbacks(mPendingCheckForTap);
                    mPendingCheckForTap.run();
                }
                removeCallbacks(mPendingCheckForTap);
                mPrivateFlags = 0;
                mPreIndex = -1;
                break;
            default:
                break;
        }
        return ret;
    }

    private int mPreIndex = -1;

    /**
     * 触摸后决定当前点击的控件
     * @param x
     * @param y
     */
    private void touchIndex(float x, float y) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (ViewHelper.pointInViewParent(child, x, y)) {
                if (mPreIndex != i) {
                    if (null != listener) {
                        listener.onIndexSelected(child, mPreIndex, i);
                    }
                    mPreIndex = i;
                    child.performClick();
                }
                break;
            }
        }
    }

    /**
     * 更新索引
     */
    public void updateIndex() {
        if (null == provider) {
            return;
        }
        removeAllViews();
        int count = provider.indexCount();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < count; i++) {
            View child = provider.inflateIndex(inflater, this, i);
            addView(child);
        }
    }
}

package cn.com.lasong.widget.touch;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Author: zhusong
 * Email: song.zhu@kascend.com
 * Date: 2020/03/04
 * Description: 可移动控件
 */
public class MoveView extends RelativeLayout {
    public MoveView(Context context) {
        this(context, null);
    }

    public MoveView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public static final int SIDE_NONE = 0;
    public static final int SIDE_BORDER = 1;
    public static final int SIDE_CENTER_X = 3;

    @IntDef({SIDE_NONE, SIDE_BORDER, SIDE_CENTER_X})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SIDE {}

    // 靠边策略
    private int mSide = SIDE_CENTER_X;
    // 获取触摸阈值判断是否滑动
    protected int mTouchSlop;
    // 获取移动控件容器大小
    protected Rect mContainerRect = new Rect();
    // 获取移动控件容器padding边距
    protected Rect mContainerPadding = new Rect();
    protected boolean mAttachWindow = false;

    // 触摸事件的记录坐标
    protected float mTouchLastX;
    protected float mTouchLastY;

    protected float mTouchDownX;
    protected float mTouchDownY;

    public MoveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateContainer();
        autoSide();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachWindow = false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        boolean isFirst = mContainerRect.isEmpty();
        if (isFirst) {
            updateContainerAndSide();
        } else {
            updateContainer();
        }
    }

    /**
     * 更新吸边策略
     * @param side
     */
    public void updateSide(@SIDE int side) {
        mSide = side;
        updateContainerAndSide();
    }

    /**
     * 更新当前位置, 继承后可以重写更新位置的方式
     * @param deltaX 偏移量X
     * @param deltaY 偏移量Y
     */
    protected void updateLayout(float deltaX, float deltaY) {
        setX(getX() + deltaX);
        setY(getY() + deltaY);
    }

    /**
     * 更新容器范围, 继承后可重写容器的范围计算
     */
    protected void updateContainer() {
        ViewGroup parent = (ViewGroup) getParent();
        if (null == parent) {
            return;
        }
        // 会计算进margin, 但是不包括padding
        parent.getHitRect(mContainerRect);
        mContainerPadding.set(parent.getPaddingLeft(), parent.getPaddingTop(),
                parent.getPaddingRight(), parent.getPaddingBottom());
    }

    /**
     * 自动靠边, 继承后重写靠边逻辑
     */
    protected void autoSide() {
        // 无靠边忽略
        if (mSide == SIDE_NONE || mContainerRect.isEmpty()) {
            return;
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        final float currentX = getX();
        final float currentY = getY();
        float newX = currentX;
        float newY = currentY;
        // 使用width()和height()是因为这个rect是包含了margin的,
        // 设置x和y是相对于父控件的, 所以不能用right/bottom来表示最大宽和高
        // 最大的x/y就是最大宽/高 减去 内边距
        final int maxX = mContainerRect.width() - mContainerPadding.right;
        // 最小的x/y就是内边距
        final int minX = mContainerPadding.left;

        final int maxY = mContainerRect.height() - mContainerPadding.bottom;
        final int minY = mContainerPadding.top;

        final int centerX = maxX / 2 - width/2;
        if (mSide == SIDE_BORDER) {
            if (currentX + width > maxX) {
                newX = maxX - width;
            } else if (newX < minX) {
                newX = minX;
            }
        } else if (mSide == SIDE_CENTER_X) {
            if (currentX > centerX) {
                newX = maxX - width;
            } else if (newX <= centerX) {
                newX = minX;
            }
        }

        if (newY + height > maxY) {
            newY = maxY - height;
        } else if (newY < minY) {
            newY = minY;
        }

        float deltaX = newX - currentX;
        float deltaY = newY - currentY;
        updateLayout(deltaX, deltaY);
    }

    /**
     * 显示
     */
    public void show(Activity activity) {
        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) {
            Context context = getContext();
            // 如果不是一个activity, 移除
            if (context instanceof Activity && activity != context) {
                remove();
            }
            // 同一个activity直接显示
            else {
                show();
                return;
            }
        }

        if (activity == null) {
            return;
        }

        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        FrameLayout contentView = decorView.findViewById(android.R.id.content);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        contentView.addView(this, params);
    }

    /**
     * 隐藏
     * 可以重写自己实现效果
     */
    public void hide() {
        setVisibility(View.GONE);
    }

    /**
     * 显示
     * 可以重写自己实现效果
     */
    protected void show() {
        setVisibility(View.VISIBLE);
    }

    /**
     * 移除
     */
    public void remove() {
        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(this);
        }
    }

    /**
     * 更新容器范围并自动靠边
     */
    protected void updateContainerAndSide() {
        updateContainer();
        autoSide();
    }


    // Step1
    // 返回true拦截事件, 不往下传递
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean ret = super.dispatchTouchEvent(ev);
        return true;
    }

    // Step2
    // 本控件内处理, 按照控件正常拦截机制拦截
    // 1. 返回true, 直接进入onTouchEvent
    // 2. 返回False会向下(子控件)传递
    //   2.1 如果没有子控件处理, 就由当前onTouchEvent处理
    //       2.1.1 如果当前控件onTouchEvent处理, 后续事件由当前控件处理
    //       2.1.1 如果当前控件onTouchEvent没有处理, 返回上一级(父控件)继续处理
    //   2.2 有子控件拦截事件, 子控件会消费后续的(MOVE&UP等)事件

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean ret = super.onInterceptTouchEvent(event);
        float x = event.getRawX();
        float y = event.getRawY();
        // 最新的位置需要更新
        mTouchLastX = x;
        mTouchLastY = y;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN : {
                mTouchDownX = x;
                mTouchDownY = y;
                break;
            }
            case MotionEvent.ACTION_MOVE : {
                float deltaX = x - mTouchDownX;
                float deltaY = y - mTouchDownY;
                // move action we get it, onTouchEvent
                // 这里就是一直都没有子控件处理触摸, 如果认定为移动阈值, 就本移动控件拦截
                // 进入onTouchEvent进行处理
                if (Math.abs(deltaX) >= mTouchSlop || Math.abs(deltaY) >= mTouchSlop) {
                    ret = true;
                }
                break;
            }
        }
        return ret;
    }

    // 记录最新的位置
    // Step3 当前View拦截到事件就进行触摸拖动
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        final int action = event.getAction();
        // 进入当前移动控件的onTouchEvent, 表示需要移动, 处理移动事件
        if (action == MotionEvent.ACTION_MOVE){
            // 计算偏移量
            float deltaX = x - mTouchLastX;
            float deltaY = y - mTouchLastY;
            // 更新最后的位置坐标
            updateLayout(deltaX, deltaY);
        }  else if (action == MotionEvent.ACTION_CANCEL
                        | action == MotionEvent.ACTION_UP) {
            autoSide();
        }
        mTouchLastX = x;
        mTouchLastY = y;
        return super.onTouchEvent(event);
    }
}

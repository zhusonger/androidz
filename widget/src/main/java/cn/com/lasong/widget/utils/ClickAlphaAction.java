package cn.com.lasong.widget.utils;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.FloatRange;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/4/30
 * Description:
 * 点击透明度处理, 用于某些控件点击事件的点击效果
 */
public class ClickAlphaAction implements View.OnTouchListener{

    @FloatRange(from = 0.0f, to = 1.0f)
    private final float clickAlpha;

    // 默认的透明度
    private float defaultAlpha = 1.0f;

    private static final int FLAG_PRE_PRESSED = 0x2;
    private static final int FLAG_PRESSED  = 0x1;
    public int mPrivateFlags;

    public ClickAlphaAction(float clickAlpha) {
        this.clickAlpha = clickAlpha;
    }

    // 延迟更新点击状态
    private Runnable mPendingCheckForTap = null;

    // 延迟重置状态
    private Runnable mUnsetPressedState = null;
    /**
     * 移除按下的计时器
     */
    private void removeTapCallback(View v) {
        if (mPendingCheckForTap != null && null != v) {
            mPrivateFlags &= ~FLAG_PRE_PRESSED;
            v.removeCallbacks(mPendingCheckForTap);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        Log.e("Test", "action : " + event.getAction());
        final boolean clickable = view.isClickable() || view.isLongClickable();
        if (clickable) {
            final float x = event.getX();
            final float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN : {
                    // 向上遍历层次结构以确定是否在滚动容器中。
                    boolean isInScrollingContainer = ViewHelper.isInScrollingContainer(view);

                    // 对于滚动容器内的视图，则将按下的反馈延迟一段时间。
                    if (isInScrollingContainer) {
                        // 设置为预按压
                        mPrivateFlags |= FLAG_PRE_PRESSED;
                        if (mPendingCheckForTap == null) {
                            mPendingCheckForTap = () -> setPressed(view, true);
                        }
                        view.postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                    } else {
                        // 不是在滚动的容器中，所以立即显示反馈
                        setPressed(view, true);
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE : {
                    // 移出按钮范围, 恢复未按下状态, 并移除计时器
                    if (!ViewHelper.pointInView(view, x, y)) {
                        removeTapCallback(view);
                        if ((mPrivateFlags & FLAG_PRESSED) != 0) {
                            setPressed(view, false);
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_CANCEL : {
                    // 取消直接更新为未按压的状态, 并移除计时器
                    setPressed(view, false);
                    removeTapCallback(view);
                    break;
                }

                case MotionEvent.ACTION_UP :  {
                    boolean prepressed = (mPrivateFlags & FLAG_PRE_PRESSED) != 0;
                    if ((mPrivateFlags & FLAG_PRESSED) != 0 || prepressed) {
                        // 获取控件的焦点
                        if (view.isFocusable() && view.isFocusableInTouchMode() && !view.isFocused()) {
                            view.requestFocus();
                        }

                        if (prepressed) {
                            // 控件正在被释放, 但是UI状态还没更新, 所以要确保显示按下的状态
                            setPressed(view, true);
                        }

                        if (mUnsetPressedState == null) {
                            mUnsetPressedState = () -> setPressed(view, false);
                        }

                        // 之前是预按下状态, 所以才更新UI, 那这里延迟隐藏一下
                        if (prepressed) {
                            view.postDelayed(mUnsetPressedState,
                                    ViewConfiguration.getPressedStateDuration());
                        } else if (!view.post(mUnsetPressedState)) {
                            // 如果重置失败, 直接运行
                            mUnsetPressedState.run();
                        }

                        removeTapCallback(view);
                    }
                    break;
                }
            }
        }
        return false;
    }


    /**
     * 设置按钮按压状态 更新UI
     * @param view  按钮
     * @param pressed 是否是按下
     */
    private void setPressed(View view, boolean pressed) {
        if (null == view) {
            return;
        }
        final boolean needsRefresh = pressed != ((mPrivateFlags & FLAG_PRESSED) == FLAG_PRESSED);
        if (pressed) {
            mPrivateFlags |= FLAG_PRESSED;
        } else {
            mPrivateFlags &= ~FLAG_PRESSED;
        }

        if (needsRefresh) {
            if (pressed) {
                // 记录下控件原来的默认度
                defaultAlpha = view.getAlpha();
                view.setAlpha(clickAlpha);
            } else {
                view.setAlpha(defaultAlpha);
            }
        }

        Log.e("Test", "setPressed : " + pressed);
    }
}

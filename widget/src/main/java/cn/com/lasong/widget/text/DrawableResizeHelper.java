package cn.com.lasong.widget.text;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

import cn.com.lasong.widget.R;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/12
 * Description:
 */
public class DrawableResizeHelper {

    /**
     * 重新设置图标大小
     * @param context
     * @param attrs
     * @param textView
     */
    public static void resizeDrawable(Context context, AttributeSet attrs, TextView textView) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ResizeTextView);
        Drawable[] drawables = textView.getCompoundDrawables();
        Drawable left = drawables[0];
        Drawable top = drawables[1];
        Drawable right = drawables[2];
        Drawable bottom = drawables[3];
        int width = ta.getDimensionPixelSize(R.styleable.ResizeTextView_drawLeftWidth,
                null != left ? left.getIntrinsicWidth() : -1);
        int height = ta.getDimensionPixelSize(R.styleable.ResizeTextView_drawLeftHeight,
                null != left ? left.getIntrinsicHeight() : -1);
        if (left != null && width > 0 && height > 0) {
            left.setBounds(0, 0, width, height);
        }

        width = ta.getDimensionPixelSize(R.styleable.ResizeTextView_drawTopWidth,
                null != top ? top.getIntrinsicWidth() : -1);
        height = ta.getDimensionPixelSize(R.styleable.ResizeTextView_drawTopHeight,
                null != top ? top.getIntrinsicWidth() : -1);
        if (top != null && width > 0 && height > 0) {
            top.setBounds(0, 0, width, height);
        }

        width = ta.getDimensionPixelSize(R.styleable.ResizeTextView_drawRightWidth,
                null != right ? right.getIntrinsicWidth() : -1);
        height = ta.getDimensionPixelSize(R.styleable.ResizeTextView_drawRightHeight,
                null != right ? right.getIntrinsicWidth() : -1);
        if (right != null && width > 0 && height > 0) {
            right.setBounds(0, 0, width, height);
        }

        width = ta.getDimensionPixelSize(R.styleable.ResizeTextView_drawBottomWidth,
                null != bottom ? bottom.getIntrinsicWidth() : -1);
        height = ta.getDimensionPixelSize(R.styleable.ResizeTextView_drawBottomHeight,
                null != bottom ? bottom.getIntrinsicWidth() : -1);
        if (bottom != null && width > 0 && height > 0) {
            bottom.setBounds(0, 0, width, height);
        }

        textView.setCompoundDrawables(left, top, right, bottom);
        ta.recycle();
    }
}

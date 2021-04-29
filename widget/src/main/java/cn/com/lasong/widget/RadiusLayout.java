package cn.com.lasong.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import cn.com.lasong.widget.utils.ViewHelper;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/4/29
 * Description:
 */
public class RadiusLayout extends RelativeLayout {
    public RadiusLayout(Context context) {
        this(context, null);
    }

    public RadiusLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Path bgPath = new Path();
    // 标记边界需要圆角的值
    int borderFlags;
    int bgColor;
    float bgRadius;
    public RadiusLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RadiusLayout);
        bgColor = ta.getColor(R.styleable.RadiusLayout_bgColor, Color.TRANSPARENT);
        bgRadius = ta.getDimension(R.styleable.RadiusLayout_bgRadius, Color.TRANSPARENT);
        borderFlags = ta.getInt(R.styleable.ShadowLayout_radiusFlags, ViewHelper.BORDER_ALL);
        ta.recycle();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (bgColor != Color.TRANSPARENT) {
            int saveCount = canvas.save();
            final float left = getPaddingLeft();
            final float top = getPaddingTop();
            final float right = getWidth() - getPaddingRight();
            final float bottom = getHeight() - getPaddingBottom();
            bgPath = ViewHelper.updateRadiusPath(bgPath, left, top, right, bottom,
                    bgRadius, bgRadius, borderFlags);

            mPaint.setColor(bgColor);
            canvas.drawPath(bgPath, mPaint);
            canvas.restoreToCount(saveCount);
        }
        super.dispatchDraw(canvas);
    }
}

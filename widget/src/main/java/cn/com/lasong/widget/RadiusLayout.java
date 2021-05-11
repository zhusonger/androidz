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
    Path borderPath = new Path();
    // 标记边界需要圆角的值
    int borderFlags;
    int bgColor;
    float bgRadius;
    int borderWidth;
    int borderColor;
    public RadiusLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RadiusLayout);
        bgColor = ta.getColor(R.styleable.RadiusLayout_bgColor, Color.TRANSPARENT);
        bgRadius = ta.getDimension(R.styleable.RadiusLayout_bgRadius, Color.TRANSPARENT);
        borderFlags = ta.getInt(R.styleable.RadiusLayout_radiusFlags, ViewHelper.BORDER_ALL);
        borderWidth = ta.getDimensionPixelSize(R.styleable.RadiusLayout_borderWidth, 0);
        borderColor = ta.getColor(R.styleable.RadiusLayout_borderColor, Color.TRANSPARENT);
        ta.recycle();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {

        int saveCount = canvas.save();

        if (bgColor != Color.TRANSPARENT) {
            final float left = getPaddingLeft();
            final float top = getPaddingTop();
            final float right = getWidth() - getPaddingRight();
            final float bottom = getHeight() - getPaddingBottom();
            bgPath = ViewHelper.updateRadiusPath(bgPath, left, top, right, bottom,
                    bgRadius, bgRadius, borderFlags);

            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(bgColor);
            canvas.drawPath(bgPath, mPaint);
        }

        if (borderWidth > 0 && borderColor != Color.TRANSPARENT) {
            final float offset = borderWidth * 1.0f / 2;
            final float left = getPaddingLeft() + offset;
            final float top = getPaddingTop() + offset;
            final float right = getWidth() - getPaddingRight() - offset;
            final float bottom = getHeight() - getPaddingBottom() - offset;
            borderPath = ViewHelper.updateRadiusPath(borderPath, left, top, right, bottom,
                    bgRadius - offset, bgRadius - offset, borderFlags);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(borderWidth);
            mPaint.setColor(borderColor);
            canvas.drawPath(borderPath, mPaint);
        }

        canvas.restoreToCount(saveCount);
        super.dispatchDraw(canvas);
    }
}

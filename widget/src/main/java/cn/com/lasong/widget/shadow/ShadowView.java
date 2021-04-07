package cn.com.lasong.widget.shadow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Author: zhusong
 * Email: song.zhu@kascend.com
 * Date: 2019/12/19
 * Description:阴影view
 */
class ShadowView extends View {

    public ShadowView(Context context) {
        this(context, null);
    }

    public ShadowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }


    final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    int shadowColor;
    float shadowRadius;
    float dx;
    float dy;

    float bgRadius;
    int bgColor;
    RectF rect = new RectF();

    public ShadowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        if (isInEditMode()) {
            shadowRadius = 10;
            shadowColor = Color.BLACK;
            bgColor = Color.WHITE;
            dx = 6;
            dy = 6;
            bgRadius = 10;
            mPaint.setColor(bgColor);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean shadow = shadowRadius > 0;
        rect.left = shadowRadius;
        rect.top = shadowRadius;
        rect.right = getWidth() - shadowRadius;
        rect.bottom = getHeight() - shadowRadius;
        if (shadow) {
            mPaint.setShadowLayer(shadowRadius, dx, dy, shadowColor);
        }

        canvas.drawRoundRect(rect, bgRadius, bgRadius, mPaint);
        if (shadow) {
            mPaint.clearShadowLayer();
        }
    }
}

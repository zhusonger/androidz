package cn.com.lasong.widget.shadow.v2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/4/29
 * Description:
 */
public class ShadowView extends View {

    final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    int shadowColor;
    float shadowRadius;
    float dx;
    float dy;

    float bgRadius;
    int bgColor;
    RectF rect = new RectF();

    public ShadowView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, mPaint);
        mPaint.setColor(bgColor);
        mPaint.setShadowLayer(shadowRadius, dx, dy, shadowColor);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int saveCount = canvas.save();
        rect.set(shadowRadius, shadowRadius, getWidth() - shadowRadius, getHeight() - shadowRadius);
        canvas.drawRoundRect(rect, bgRadius, bgRadius, mPaint);
        canvas.restoreToCount(saveCount);
    }
}

package cn.com.lasong.widget.shadow.v2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import cn.com.lasong.widget.utils.ViewHelper;

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
    Path bgPath = new Path();
    // 标记边界需要圆角的值
    int borderFlags = ViewHelper.BORDER_ALL;
    public ShadowView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, mPaint);
        setWillNotDraw(false);
    }

    protected void update() {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int saveCount = canvas.save();
        final float left = shadowRadius + getPaddingLeft();
        final float top = shadowRadius + getPaddingTop();
        final float right = getWidth() - getPaddingRight() - shadowRadius;
        final float bottom = getHeight() - getPaddingBottom() - shadowRadius;
        bgPath = ViewHelper.updateRadiusPath(bgPath, left, top, right, bottom,
                bgRadius, bgRadius, borderFlags);

        mPaint.setColor(bgColor);
        if (shadowColor != Color.TRANSPARENT) {
            mPaint.setShadowLayer(shadowRadius, dx, dy, shadowColor);
        }

        canvas.drawPath(bgPath, mPaint);

        if (shadowColor != Color.TRANSPARENT) {
            mPaint.clearShadowLayer();
        }
        canvas.restoreToCount(saveCount);
    }


}

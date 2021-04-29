package cn.com.lasong.widget.shadow.v2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    Path bgPath = new Path();
    // LT 0001
    // RT 0010
    // RB 0100
    // LB 1000
    // ALL 1111
    public static final int BORDER_LEFT_TOP = 0b0001;
    public static final int BORDER_RIGHT_TOP = 0b0010;
    public static final int BORDER_RIGHT_BOTTOM = 0b0100;
    public static final int BORDER_LEFT_BOTTOM = 0b1000;
    public static final int BORDER_ALL = 0b1111;
    // 标记边界需要圆角的值
    int borderFlags = BORDER_ALL;
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
        final float left = bgRadius;
        final float top = bgRadius;
        final float right = getWidth() - bgRadius;
        final float bottom = getHeight() - bgRadius;
        final boolean lt = (borderFlags & BORDER_LEFT_TOP) > 0;
        final boolean rt = (borderFlags & BORDER_RIGHT_TOP) > 0;
        final boolean rb = (borderFlags & BORDER_RIGHT_BOTTOM) > 0;
        final boolean lb = (borderFlags & BORDER_LEFT_BOTTOM) > 0;
        updatePath(left, top, right, bottom, bgRadius, bgRadius,
                lt, rt, rb, lb);

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

    /**
     * 更新路径
     */
    private void updatePath(
            float left, float top, float right, float bottom, float rx, float ry,
            boolean lt, boolean rt, boolean rb, boolean lb){
        bgPath.reset();
        if (rx < 0) rx = 0;
        if (ry < 0) ry = 0;
        float width = right - left;
        float height = bottom - top;
        if (rx > width / 2) rx = width / 2;
        if (ry > height / 2) ry = height / 2;
        float widthMinusCorners = (width - (2 * rx));
        float heightMinusCorners = (height - (2 * ry));

        bgPath.moveTo(right, top + ry);
        if (rt)
            bgPath.rQuadTo(0, -ry, -rx, -ry);//top-right corner
        else{
            bgPath.rLineTo(0, -ry);
            bgPath.rLineTo(-rx,0);
        }
        bgPath.rLineTo(-widthMinusCorners, 0);
        if (lt)
            bgPath.rQuadTo(-rx, 0, -rx, ry); //top-left corner
        else{
            bgPath.rLineTo(-rx, 0);
            bgPath.rLineTo(0,ry);
        }
        bgPath.rLineTo(0, heightMinusCorners);

        if (lb)
            bgPath.rQuadTo(0, ry, rx, ry);//bottom-left corner
        else{
            bgPath.rLineTo(0, ry);
            bgPath.rLineTo(rx,0);
        }

        bgPath.rLineTo(widthMinusCorners, 0);
        if (rb)
            bgPath.rQuadTo(rx, 0, rx, -ry); //bottom-right corner
        else{
            bgPath.rLineTo(rx,0);
            bgPath.rLineTo(0, -ry);
        }

        bgPath.rLineTo(0, -heightMinusCorners);

        bgPath.close();//Given close, last lineto can be removed.
    }
}

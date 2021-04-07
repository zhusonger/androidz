package cn.com.lasong.widget.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

/**
 * VerticalImageSpan
 *
 * @author KenChung
 */
public class VerticalImageSpan extends ImageSpan {
    private final Paint.FontMetricsInt mFontMetricsInt = new Paint.FontMetricsInt();

    private boolean mDrawDefault = false;

    public VerticalImageSpan(Drawable drawable) {
        super(drawable);
    }

    public VerticalImageSpan(Drawable drawable, String src) {
        super(drawable, src);
    }

    public void setDrawDefault(boolean drawDefault) {
        mDrawDefault = drawDefault;
    }
    public int getSize(Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt fontMetricsInt) {
        if (mDrawDefault) {
            return super.getSize(paint, text, start, end, fontMetricsInt);
        }
        Drawable drawable = getDrawable();
        Rect rect = drawable.getBounds();
        if (rect.isEmpty()) {
            return super.getSize(paint, text, start, end, fontMetricsInt);
        }
        if (fontMetricsInt != null) {
            Paint.FontMetricsInt fmPaint = paint.getFontMetricsInt();
            int fontHeight = fmPaint.bottom - fmPaint.top;
            int drHeight = rect.bottom - rect.top;

            int top = drHeight / 2 - fontHeight / 4;
            int bottom = drHeight / 2 + fontHeight / 4;

            fontMetricsInt.ascent = -bottom;
            fontMetricsInt.top = -bottom;
            fontMetricsInt.bottom = top;
            fontMetricsInt.descent = top;
        }
        return rect.right;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, Paint paint) {
        if (mDrawDefault) {
            super.draw(canvas, text, start, end, x, top, y, bottom, paint);
            return;
        }
        Drawable drawable = getDrawable();
        Rect rect = drawable.getBounds();
        if (rect.isEmpty()) {
            super.draw(canvas, text, start, end, x, top, y, bottom, paint);
            return;
        }
        paint.getFontMetricsInt(mFontMetricsInt);
        int textHeight = mFontMetricsInt.descent - mFontMetricsInt.ascent;
        int offset = (textHeight - drawable.getBounds().height()) / 2;
        int iconTop = y + mFontMetricsInt.ascent + offset;
        canvas.translate(x, iconTop);
        drawable.draw(canvas);
        canvas.translate(-x, -iconTop);
    }
}

package cn.com.lasong.widget.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import java.lang.ref.WeakReference;

/**
 * Author: zhusong
 * Email: song.zhu@kascend.com
 * Date: 2019/11/26
 * Description: 网络图片
 */
public class UrlDrawable extends BitmapDrawable {

    private UrlDrawable() {
    }
    protected Drawable mDrawable;
    public UrlDrawable(Drawable drawable) {
        mDrawable = drawable;
        if (null != drawable) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        updateBounds();
    }

    protected int mTargetHeight;
    protected WeakReference<TextView> mTextViewRef;
    public UrlDrawable(TextView textView) {
        this(textView, null);
    }
    public UrlDrawable(TextView textView, Drawable drawable) {
        this(drawable);
        mTextViewRef = new WeakReference<>(textView);
    }

    public void setTargetHeight(int targetHeight) {
        this.mTargetHeight = targetHeight;
    }

    /**
     * 获取textview文字高度
     * @return
     */
    protected int getFontHeight() {
        TextView textView = null;
        if (null != mTextViewRef && null != mTextViewRef.get()) {
            textView = mTextViewRef.get();
        }
        if (null == textView) {
            return -1;
        }
        Paint.FontMetricsInt fmPaint = textView.getPaint().getFontMetricsInt();
        return fmPaint.bottom - fmPaint.top;
    }

    private String mUrl;
    public void loadUrl(String url) {
        // 这里可以替换成其他的下载网络图片的方式
        mUrl = url;
    }

    public boolean isGif() {
        return !TextUtils.isEmpty(mUrl) && mUrl.toLowerCase().endsWith(".gif");
    }

    protected void postInvalidate() {
        TextView textView = null;
        if (null != mTextViewRef) {
            textView = mTextViewRef.get();
        }
        if (null != textView) {
            textView.invalidate();
            textView.setText(textView.getText());
        }
    }

    protected TextView getTextView() {
        return null != mTextViewRef ? mTextViewRef.get() : null;
    }

    protected void release() {
        updateDrawable(null);
        if (null != mTextViewRef) {
            mTextViewRef.clear();
            mTextViewRef = null;
        }
    }
    protected void updateDrawable(Drawable d) {
        if (mDrawable == d && d != null) {
            return;
        }
        if (mDrawable != null) {
            mDrawable.setCallback(null);
            mDrawable.setVisible(false, false);
        }

        mDrawable = d;

        if (d != null) {
            d.setCallback(new Callback() {
                @Override
                public void invalidateDrawable(@NonNull Drawable who) {
                    TextView textView = getTextView();
                    if (null == textView) {
                        return;
                    }
                    if (ViewCompat.isAttachedToWindow(textView)) {
                        textView.invalidate();
                    }
                }

                @Override
                public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
                    TextView textView = getTextView();
                    if (null == textView) {
                        return;
                    }
                    textView.postDelayed(what, when);
                }

                @Override
                public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
                    TextView textView = getTextView();
                    if (null == textView) {
                        return;
                    }
                    textView.removeCallbacks(what);
                }
            });

            final int width = d.getIntrinsicWidth();
            final int height = d.getIntrinsicHeight();
            int targetWidth = width;
            int targetHeight = height;
            final int fontHeight = getFontHeight();
            int scaleHeight;
            if (mTargetHeight > 0) {
                scaleHeight = mTargetHeight;
            }
            // 超过字体高度就使用字体, 否则用图片高度
            else {
                scaleHeight = targetHeight > fontHeight ? fontHeight : targetHeight;
            }
            if (scaleHeight > 0) {
                float scale = scaleHeight * 1.0f / height;
                targetHeight = (int) (height * scale);
                targetWidth = (int) (width * scale);
            }
            d.setBounds(0, 0, targetWidth, targetHeight);
            updateBounds();
            d.setVisible(true, true);
            postInvalidate();
        }
    }

    protected void updateBounds() {
        if (null != mDrawable) {
            setBounds(mDrawable.getBounds());
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (null != mDrawable) {
            mDrawable.draw(canvas);
        }
    }
}

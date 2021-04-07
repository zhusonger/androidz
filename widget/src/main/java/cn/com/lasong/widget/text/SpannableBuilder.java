package cn.com.lasong.widget.text;

import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.style.ImageSpan;
import android.widget.TextView;

import java.util.List;

/**
 * Author: zhusong
 * Email: song.zhu@kascend.com
 * Date: 2019/11/26
 * Description: 图文混排
 */
public class SpannableBuilder extends SpannableStringBuilder {

    public SpannableBuilder() {
        super();
    }

    public SpannableBuilder(CharSequence text) {
        super(text);
    }

    public SpannableBuilder(CharSequence text, int start, int end) {
        super(text, start, end);
    }


    public SpannableBuilder appendUrlImage(UrlDrawable drawable) {
        return appendUrlImage(drawable, "");
    }
    public SpannableBuilder appendUrlImage(UrlDrawable drawable, String source) {
        int start = length();
        append("\uFFFC");
        int end = length();
        setSpan(new VerticalImageSpan(drawable, source), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return this;
    }

    public SpannableBuilder append(CharSequence text, List<Object> spans) {
        append(text);
        for (Object span : spans) {
            setSpan(span, length() - text.length(), length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return this;
    }

    public SpannableBuilder append(CharSequence text, Object span) {
        append(text);
        setSpan(span, length() - text.length(), length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return this;
    }

    public boolean isImageSpanStart() {
        VerticalImageSpan[] imageSpans = getSpans(0, length(), VerticalImageSpan.class);
        if (null == imageSpans || imageSpans.length <= 0) {
            return false;
        }

        int startIndex = getSpanStart(imageSpans[0]);
        return startIndex == 0;
    }

    /**
     * 释放资源文本中的图文混排
     * @param textView
     */
    public static void release(TextView textView) {
        if (null == textView) {
            return;
        }

        CharSequence text = textView.getText();
        if (text instanceof SpannedString) {
            SpannedString spanned = (SpannedString) text;
            VerticalImageSpan[] imageSpans = spanned.getSpans(0, spanned.length(), VerticalImageSpan.class);
            if (null != imageSpans && imageSpans.length > 0) {
                for (VerticalImageSpan span : imageSpans) {
                    Drawable drawable = span.getDrawable();
                    if (drawable instanceof UrlDrawable) {
                        ((UrlDrawable) drawable).release();
                    }
                }
            }

            ImageSpan[] imageSpans2 = spanned.getSpans(0, spanned.length(), ImageSpan.class);
            if (null != imageSpans2 && imageSpans2.length > 0) {
                for (ImageSpan span : imageSpans2) {
                    Drawable drawable = span.getDrawable();
                    if (null != drawable) {
                        drawable.setCallback(null);
                        drawable.setVisible(false, false);
                    }
                }
            }
        }

    }
}

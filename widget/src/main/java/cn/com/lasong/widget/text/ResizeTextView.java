package cn.com.lasong.widget.text;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckedTextView;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/08/12
 * Description: 可控制图标大小的文本控件
 */
public class ResizeTextView extends AppCompatCheckedTextView {
    public ResizeTextView(Context context) {
        this(context, null);
    }

    public ResizeTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ResizeTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        DrawableResizeHelper.resizeDrawable(context, attrs, this);
    }
}

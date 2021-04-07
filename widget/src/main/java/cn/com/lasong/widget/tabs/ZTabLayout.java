package cn.com.lasong.widget.tabs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayout;

import cn.com.lasong.widget.R;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/3/1
 * Description:
 */
public class ZTabLayout extends TabLayout {

    private int customViewResId;
    public ZTabLayout(@NonNull Context context) {
        this(context, null);
    }

    public ZTabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZTabLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ZTabLayout);
        customViewResId = ta.getResourceId(R.styleable.ZTabLayout_custom_view, NO_ID);
        ta.recycle();
    }

    @NonNull
    @Override
    public Tab newTab() {
        Tab tab = super.newTab();
        if (customViewResId != NO_ID) {
            tab.setCustomView(customViewResId);
        }
        return tab;
    }
}

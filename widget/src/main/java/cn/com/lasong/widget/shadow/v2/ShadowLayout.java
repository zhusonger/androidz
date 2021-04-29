package cn.com.lasong.widget.shadow.v2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import cn.com.lasong.widget.R;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/4/29
 * Description: 阴影控件版本2, 不用考虑阴影边界裁剪问题, 并且根据内容自适应
 * 阴影不裁剪的前提是当前控件的父控件的范围足够,
 * 如果不够只能继续往上层父控件设置clipChildren、clipToPadding属性为false
 */
public class ShadowLayout extends RelativeLayout {
    public ShadowLayout(@NonNull Context context) {
        this(context, null);
    }

    public ShadowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private ShadowView shadow;
    private ShadowMode shadowmode;

    public ShadowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        shadow = new ShadowView(context);
        shadow.setLayoutParams(generateDefaultLayoutParams());
        shadow.setTranslationZ(0);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ShadowLayout);
        shadow.bgColor = ta.getColor(R.styleable.ShadowLayout_bgColor, Color.TRANSPARENT);
        shadow.bgRadius = ta.getDimension(R.styleable.ShadowLayout_bgRadius, 0);
        shadow.mPaint.setColor(shadow.bgColor);
        shadow.dx = ta.getDimension(R.styleable.ShadowLayout_shadowDx, 0);
        shadow.dy = ta.getDimension(R.styleable.ShadowLayout_shadowDy, 0);
        shadow.shadowColor = ta.getColor(R.styleable.ShadowLayout_shadowColor, Color.TRANSPARENT);
        shadow.shadowRadius = ta.getDimension(R.styleable.ShadowLayout_shadowRadius, 0);
        int index = ta.getInt(R.styleable.ShadowLayout_shadowMode, -1);
        ta.recycle();
        if (index >= 0) {
            shadowmode = ShadowMode.values()[index];
        } else {
            shadowmode = ShadowMode.WRAP_ALL;
        }
        setShadowMode(shadowmode);
    }

    /**
     * 切换阴影模式
     * @param mode
     */
    public void setShadowMode(ShadowMode mode) {
        LayoutParams params = (LayoutParams) shadow.getLayoutParams();
        final int shadowRadius = (int) shadow.shadowRadius;
        if (mode == ShadowMode.WRAP_ALL) {
            params.width = LayoutParams.WRAP_CONTENT;
            params.height = LayoutParams.WRAP_CONTENT;
            int count = getChildCount();
            View anchor = null;
            for (int i = 0; i < count; i++) {
                View view = getChildAt(i);
                if (view instanceof ShadowView) {
                    continue;
                }
                anchor = view;
                break;
            }
            if (null != anchor) {
                params.addRule(RelativeLayout.ALIGN_LEFT, anchor.getId());
                params.addRule(RelativeLayout.ALIGN_TOP, anchor.getId());
                params.addRule(RelativeLayout.ALIGN_RIGHT, anchor.getId());
                params.addRule(RelativeLayout.ALIGN_BOTTOM, anchor.getId());
            }
        } else if (mode == ShadowMode.FILL_ALL) {
            params.width = LayoutParams.MATCH_PARENT;
            params.height = LayoutParams.MATCH_PARENT;
        } else if (mode == ShadowMode.FILL_W_WARP_H) {
            params.width = LayoutParams.MATCH_PARENT;
            params.height = LayoutParams.WRAP_CONTENT;
            int count = getChildCount();
            View anchor = null;
            for (int i = 0; i < count; i++) {
                View view = getChildAt(i);
                if (view instanceof ShadowView) {
                    continue;
                }
                anchor = view;
                break;
            }
            if (null != anchor) {
                params.addRule(RelativeLayout.ALIGN_TOP, anchor.getId());
                params.addRule(RelativeLayout.ALIGN_BOTTOM, anchor.getId());
            }
        } else if (mode == ShadowMode.WARP_W_FILL_H) {
            params.width = LayoutParams.WRAP_CONTENT;
            params.height = LayoutParams.MATCH_PARENT;
            int count = getChildCount();
            View anchor = null;
            for (int i = 0; i < count; i++) {
                View view = getChildAt(i);
                if (view instanceof ShadowView) {
                    continue;
                }
                anchor = view;
                break;
            }
            if (null != anchor) {
                params.addRule(RelativeLayout.ALIGN_LEFT, anchor.getId());
                params.addRule(RelativeLayout.ALIGN_RIGHT, anchor.getId());
            }
        }
        params.setMargins(-shadowRadius, -shadowRadius, -shadowRadius, -shadowRadius);
        final ShadowMode cache = this.shadowmode;
        this.shadowmode = mode;
        if (cache != mode) {
            requestLayout();
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ShadowLayout can host only one direct child");
        }

        if (child.getId() == NO_ID) {
            throw new IllegalStateException("ShadowLayout child id must be set");
        }

        child.setTranslationZ(1);
        super.addView(child, index, params);
        super.addView(shadow, 0, shadow.getLayoutParams());
        setShadowMode(shadowmode);
    }

    private boolean cacheClipChildren = false;
    private boolean cacheClipToPadding = false;
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setClipChildren(false);
        setClipToPadding(false);
        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) {
            cacheClipChildren = ((ViewGroup) parent).getClipChildren();
            cacheClipToPadding = ((ViewGroup) parent).getClipToPadding();
            ((ViewGroup) parent).setClipChildren(false);
            ((ViewGroup) parent).setClipToPadding(false);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).setClipChildren(cacheClipChildren);
            ((ViewGroup) parent).setClipToPadding(cacheClipToPadding);
        }
    }
}

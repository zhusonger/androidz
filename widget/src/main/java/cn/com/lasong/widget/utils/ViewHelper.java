package cn.com.lasong.widget.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.util.Pools;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/11/16
 * Description:
 */
public class ViewHelper {

    private static final ThreadLocal<Matrix> sMatrix = new ThreadLocal<>();
    private static final ThreadLocal<RectF> sRectF = new ThreadLocal<>();

    /**
     * This is a port of the common
     * {@link ViewGroup#offsetDescendantRectToMyCoords(View, Rect)}
     * from the framework, but adapted to take transformations into account. The result
     * will be the bounding rect of the real transformed rect.
     *
     * @param descendant view defining the original coordinate system of rect
     * @param rect (in/out) the rect to offset from descendant to this view's coordinate system
     */
    static void offsetDescendantRect(ViewGroup parent, View descendant, Rect rect) {
        Matrix m = sMatrix.get();
        if (m == null) {
            m = new Matrix();
            sMatrix.set(m);
        } else {
            m.reset();
        }

        offsetDescendantMatrix(parent, descendant, m);

        RectF rectF = sRectF.get();
        if (rectF == null) {
            rectF = new RectF();
            sRectF.set(rectF);
        }
        rectF.set(rect);
        m.mapRect(rectF);
        rect.set((int) (rectF.left + 0.5f), (int) (rectF.top + 0.5f),
                (int) (rectF.right + 0.5f), (int) (rectF.bottom + 0.5f));
    }

    /**
     * Retrieve the transformed bounding rect of an arbitrary descendant view.
     * This does not need to be a direct child.
     *
     * @param descendant descendant view to reference
     * @param out rect to set to the bounds of the descendant view
     */
    public static void getDescendantRect(ViewGroup parent, View descendant, Rect out) {
        out.set(0, 0, descendant.getWidth(), descendant.getHeight());
        offsetDescendantRect(parent, descendant, out);
    }

    private static void offsetDescendantMatrix(ViewParent target, View view, Matrix m) {
        final ViewParent parent = view.getParent();
        if (parent instanceof View && parent != target) {
            final View vp = (View) parent;
            offsetDescendantMatrix(target, vp, m);
            m.preTranslate(-vp.getScrollX(), -vp.getScrollY());
        }

        m.preTranslate(view.getLeft(), view.getTop());

        if (!view.getMatrix().isIdentity()) {
            m.preConcat(view.getMatrix());
        }
    }



    private static final Pools.Pool<Rect> sRectPool = new Pools.SynchronizedPool<>(12);

    @NonNull
    private static Rect acquireTempRect() {
        Rect rect = sRectPool.acquire();
        if (rect == null) {
            rect = new Rect();
        }
        return rect;
    }

    private static void releaseTempRect(@NonNull Rect rect) {
        rect.setEmpty();
        sRectPool.release(rect);
    }

    /**
     * Check if a given point in the CoordinatorLayout's coordinates are within the view bounds
     * of the given direct child view.
     *
     * @param child child view to test
     * @param x X coordinate to test, in the CoordinatorLayout's coordinate system
     * @param y Y coordinate to test, in the CoordinatorLayout's coordinate system
     * @return true if the point is within the child view's bounds, false otherwise
     */
    public static boolean isPointInChildBounds(ViewGroup parent, @NonNull View child, int x, int y) {
        final Rect r = acquireTempRect();
        getDescendantRect(parent, child, r);
        try {
            return r.contains(x, y);
        } finally {
            releaseTempRect(r);
        }
    }

    /**
     * Returns the {@link ColorStateList} from the given {@link TypedArray} attributes. The resource
     * can include themeable attributes, regardless of API level.
     */
    @Nullable
    public static ColorStateList getColorStateList(
            @NonNull Context context, @NonNull TypedArray attributes, @StyleableRes int index) {
        if (attributes.hasValue(index)) {
            int resourceId = attributes.getResourceId(index, 0);
            if (resourceId != 0) {
                ColorStateList value = AppCompatResources.getColorStateList(context, resourceId);
                if (value != null) {
                    return value;
                }
            }
        }

        // Reading a single color with getColorStateList() on API 15 and below doesn't always correctly
        // read the value. Instead we'll first try to read the color directly here.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            int color = attributes.getColor(index, -1);
            if (color != -1) {
                return ColorStateList.valueOf(color);
            }
        }

        return attributes.getColorStateList(index);
    }



    private static boolean INIT = false;
    private static int STATUS_BAR_HEIGHT = 50;

    private final static String STATUS_BAR_DEF_PACKAGE = "android";
    private final static String STATUS_BAR_DEF_TYPE = "dimen";
    private final static String STATUS_BAR_NAME = "status_bar_height";

    public static synchronized int getStatusBarHeight(final Context context) {
        if (!INIT) {
            int resourceId = context.getResources().
                    getIdentifier(STATUS_BAR_NAME, STATUS_BAR_DEF_TYPE, STATUS_BAR_DEF_PACKAGE);
            if (resourceId > 0) {
                STATUS_BAR_HEIGHT = context.getResources().getDimensionPixelSize(resourceId);
                INIT = true;
            }
        }

        return STATUS_BAR_HEIGHT;
    }

    /**
     * 设置activity为状态栏透明
     */
    public static void transparentStatusBar(@NonNull Activity activity) {
        //5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        //两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        //导航栏颜色也可以正常设置
        // window.setNavigationBarColor(Color.TRANSPARENT);
    }


    /**
     * 设置状态栏间距
     * @param view
     */
    public static void fitStatusBar(View view) {
        fitStatusBar(view, 0, true);
    }
    public static void fitStatusBar(View view, boolean padding) {
        fitStatusBar(view, 0, padding);
    }
    public static void fitStatusBar(View view, int offset) {
        fitStatusBar(view, offset, true);
    }
    public static void fitStatusBar(View view, int offset, boolean padding) {
        if (null == view) {
            return;
        }
        if (padding) {
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop() + getStatusBarHeight(view.getContext()) + offset,
                    view.getPaddingRight(), view.getPaddingBottom());
        } else {
            ViewParent parent = view.getParent();
            if (parent instanceof ViewGroup && view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                lp.topMargin += (getStatusBarHeight(view.getContext()) + offset);
                view.setLayoutParams(lp);
            }
        }
    }
}

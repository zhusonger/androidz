package cn.com.lasong.widget.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import cn.com.lasong.widget.R;
import cn.com.lasong.widget.utils.ViewHelper;


/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/11/13
 * Description:
 * 顶部可滑动dialog
 */
public class TopSheetDialog extends AppCompatDialog {

    private TopSheetBehavior<FrameLayout> behavior;

    private FrameLayout container;

    boolean dismissWithAnimation;

    boolean cancelable = true;
    private boolean canceledOnTouchOutside = true;
    private boolean canceledOnTouchOutsideSet;

    // 是否消费触摸(不消费就传递到dialog下面)
    private boolean consumeTouch = true;

    public TopSheetDialog(@NonNull Context context) {
        this(context, 0);
    }

    public TopSheetDialog(@NonNull Context context, @StyleRes int theme) {
        super(context, getThemeResId(context, theme));
        // We hide the title bar for any style configuration. Otherwise, there will be a gap
        // above the bottom sheet when it is expanded.
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    protected TopSheetDialog(
            @NonNull Context context, boolean cancelable, DialogInterface.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        this.cancelable = cancelable;
    }

    @Override
    public void setContentView(@LayoutRes int layoutResId) {
        super.setContentView(wrapInTopSheet(layoutResId, null, null));
        onContentViewCreated(container);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(wrapInTopSheet(0, view, null));
        onContentViewCreated(container);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(wrapInTopSheet(0, view, params));
        onContentViewCreated(container);
    }

    /**
     * view设置完成之后
     * @param parent
     */
    protected void onContentViewCreated(View parent) {

    }

    /**
     * 获取容器布局
     * @return
     */
    protected final FrameLayout getContainer() {
        return container;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        boolean ret = super.onTouchEvent(event);
        if (null != container && !consumeTouch) {
            FrameLayout bottomSheet = (FrameLayout) container.findViewById(R.id.design_top_sheet);
            if (null != bottomSheet) {
                ret &= ViewHelper.isPointInChildBounds(container, bottomSheet, (int)event.getX(), (int)event.getY());
            }
            if (!ret && getRealContext() instanceof Activity) {
                Activity activity = (Activity) getRealContext();
                MotionEvent e = MotionEvent.obtain(event);
                activity.dispatchTouchEvent(e);
                e.recycle();
            }
        }
        return ret;
    }

    /**
     * 传递过来的Context会被包装成ContextThemeWrapper，调用getContext()无法获取传递过来的真实context
     *
     * @return 真实的Context
     */
    protected Context getRealContext() {
        Context context = getContext();
        if (context instanceof ContextWrapper) {
            return ((ContextWrapper) context).getBaseContext();
        }
        return context;
    }

    @Override
    public void setCancelable(boolean cancelable) {
        super.setCancelable(cancelable);
        if (this.cancelable != cancelable) {
            this.cancelable = cancelable;
            if (behavior != null) {
                behavior.setHideable(cancelable);
            }
        }
    }

    public void setConsumeTouch(boolean consumeTouch) {
        this.consumeTouch = consumeTouch;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (behavior != null && behavior.getState() == TopSheetBehavior.STATE_HIDDEN) {
            behavior.setState(TopSheetBehavior.STATE_COLLAPSED);
        }
    }

    /**
     * This function can be called from a few different use cases, including Swiping the dialog down
     * or calling `dismiss()` from a `BottomSheetDialogFragment`, tapping outside a dialog, etc...
     *
     * <p>The default animation to dismiss this dialog is a fade-out transition through a
     * windowAnimation. Call {@link TopSheetDialog#setDismissWithAnimation(boolean)} if you want to utilize the
     * BottomSheet animation instead.
     *
     * <p>If this function is called from a swipe down interaction, or dismissWithAnimation is false,
     * then keep the default behavior.
     *
     * <p>Else, since this is a terminal event which will finish this dialog, we override the attached
     * {@link TopSheetBehavior.TopSheetCallback} to call this function, after {@link
     * TopSheetBehavior#STATE_HIDDEN} is set. This will enforce the swipe down animation before
     * canceling this dialog.
     */
    @Override
    public void cancel() {
        TopSheetBehavior<FrameLayout> behavior = getBehavior();

        if (!dismissWithAnimation || behavior.getState() == TopSheetBehavior.STATE_HIDDEN
                || behavior.getState() == TopSheetBehavior.STATE_COLLAPSED) {
            super.cancel();
        } else {
            behavior.setState(TopSheetBehavior.STATE_HIDDEN);
        }
    }

    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(cancel);
        if (cancel && !cancelable) {
            cancelable = true;
        }
        canceledOnTouchOutside = cancel;
        canceledOnTouchOutsideSet = true;
    }

    @NonNull
    public TopSheetBehavior<FrameLayout> getBehavior() {
        if (behavior == null) {
            // The content hasn't been set, so the behavior doesn't exist yet. Let's create it.
            ensureContainerAndBehavior();
        }
        return behavior;
    }

    /**
     * Set to perform the swipe down animation when dismissing instead of the window animation for the
     * dialog.
     *
     * @param dismissWithAnimation True if swipe down animation should be used when dismissing.
     */
    public void setDismissWithAnimation(boolean dismissWithAnimation) {
        this.dismissWithAnimation = dismissWithAnimation;
    }

    /**
     * Returns if dismissing will perform the swipe down animation on the bottom sheet, rather than
     * the window animation for the dialog.
     */
    public boolean getDismissWithAnimation() {
        return dismissWithAnimation;
    }

    /** Creates the container layout which must exist to find the behavior */
    private FrameLayout ensureContainerAndBehavior() {
        if (container == null) {
            container =
                    (FrameLayout) View.inflate(getContext(), R.layout.design_top_sheet_dialog, null);

            FrameLayout bottomSheet = (FrameLayout) container.findViewById(R.id.design_top_sheet);
            behavior = TopSheetBehavior.from(bottomSheet);
            behavior.addTopSheetCallback(topSheetCallback);
            behavior.setHideable(cancelable);
        }
        return container;
    }

    private View wrapInTopSheet(
            int layoutResId, @Nullable View view, @Nullable ViewGroup.LayoutParams params) {
        ensureContainerAndBehavior();
        CoordinatorLayout coordinator = (CoordinatorLayout) container.findViewById(R.id.coordinator);
        if (layoutResId != 0 && view == null) {
            view = getLayoutInflater().inflate(layoutResId, coordinator, false);
        }

        FrameLayout bottomSheet = (FrameLayout) container.findViewById(R.id.design_top_sheet);
        if (params == null) {
            bottomSheet.addView(view);
        } else {
            bottomSheet.addView(view, params);
        }
        // We treat the CoordinatorLayout as outside the dialog though it is technically inside
        coordinator
                .findViewById(R.id.touch_outside)
                .setOnTouchListener(
                        new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                // Consume the event and prevent it from falling through only if cancelable is true
                                if (cancelable && isShowing() && shouldWindowCloseOnTouchOutside()) {
                                    cancel();
                                    return true;
                                }
                                return consumeTouch;
                            }
                        });
        // Handle accessibility events
        ViewCompat.setAccessibilityDelegate(
                bottomSheet,
                new AccessibilityDelegateCompat() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(
                            View host, @NonNull AccessibilityNodeInfoCompat info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        if (cancelable) {
                            info.addAction(AccessibilityNodeInfoCompat.ACTION_DISMISS);
                            info.setDismissable(true);
                        } else {
                            info.setDismissable(false);
                        }
                    }

                    @Override
                    public boolean performAccessibilityAction(View host, int action, Bundle args) {
                        if (action == AccessibilityNodeInfoCompat.ACTION_DISMISS && cancelable) {
                            cancel();
                            return true;
                        }
                        return super.performAccessibilityAction(host, action, args);
                    }
                });
        bottomSheet.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        // Consume the event and prevent it from falling through
                        return true;
                    }
                });
        return container;
    }

    boolean shouldWindowCloseOnTouchOutside() {
        if (!canceledOnTouchOutsideSet) {
            TypedArray a =
                    getContext()
                            .obtainStyledAttributes(new int[] {android.R.attr.windowCloseOnTouchOutside});
            canceledOnTouchOutside = a.getBoolean(0, true);
            a.recycle();
            canceledOnTouchOutsideSet = true;
        }
        return canceledOnTouchOutside;
    }

    private static int getThemeResId(@NonNull Context context, int themeId) {
        if (themeId == 0) {
            // If the provided theme is 0, then retrieve the dialogTheme from our theme
            // bottomSheetDialogTheme is not provided; we default to our light theme
            themeId = R.style.Theme_Design_Light_TopSheetDialog;
        }
        return themeId;
    }

    void removeDefaultCallback() {
        behavior.removeTopSheetCallback(topSheetCallback);
    }

    @NonNull
    private TopSheetBehavior.TopSheetCallback topSheetCallback =
            new TopSheetBehavior.TopSheetCallback() {
                @Override
                public void onStateChanged(
                        @NonNull View bottomSheet, @TopSheetBehavior.State int newState) {
                    Log.d("Test", "onStateChanged : " + newState);
                    if (newState == TopSheetBehavior.STATE_HIDDEN
                        || newState == TopSheetBehavior.STATE_COLLAPSED) {
                        cancel();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
            };
}

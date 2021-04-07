package cn.com.lasong.widget.dialog;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.math.MathUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.ViewDragHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.com.lasong.widget.R;


/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/11/13
 * Description:
 */
/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * An interaction behavior plugin for a child view of {@link CoordinatorLayout} to make it work as a
 * top sheet.
 *
 * <p>To send useful accessibility events, set a title on top sheets that are windows or are
 * window-like. For BottomSheetDialog use {@link TopSheetDialog#setTitle(int)}, and for
 * BottomSheetDialogFragment use {@link ViewCompat#setAccessibilityPaneTitle(View, CharSequence)}.
 */
public class TopSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    /** Callback for monitoring events about top sheets. */
    public abstract static class TopSheetCallback {

        /**
         * Called when the top sheet changes its state.
         *
         * @param bottomSheet The top sheet view.
         * @param newState The new state. This will be one of {@link #STATE_DRAGGING}, {@link
         *     #STATE_SETTLING}, {@link #STATE_EXPANDED}, {@link #STATE_COLLAPSED}, {@link
         *     #STATE_HIDDEN}.
         */
        public abstract void onStateChanged(@NonNull View bottomSheet, @State int newState);

        /**
         * Called when the top sheet is being dragged.
         *
         * @param bottomSheet The top sheet view.
         * @param slideOffset The new offset of this top sheet within [-1,1] range. Offset increases
         *     as this top sheet is moving upward. From 0 to 1 the sheet is between collapsed and
         *     expanded states and from -1 to 0 it is between hidden and collapsed states.
         */
        public abstract void onSlide(@NonNull View bottomSheet, float slideOffset);
    }

    /** The top sheet is dragging. */
    public static final int STATE_DRAGGING = 1; // 正在通过手势向上/向下拖动工作表。

    /** The top sheet is settling. */
    public static final int STATE_SETTLING = 2; // 由于以编程方式设置了其状态，因此工作表正在上/下动画。

    /** The top sheet is expanded. */
    public static final int STATE_EXPANDED = 3; // 完全展开

    /** The top sheet is collapsed. */
    public static final int STATE_COLLAPSED = 4; // 收缩状态

    /** The top sheet is hidden. */
    public static final int STATE_HIDDEN = 5; // 工作表处于隐藏状态，只能以编程方式重新显示。


    /** @hide */
    @IntDef({
            STATE_EXPANDED,
            STATE_COLLAPSED,
            STATE_DRAGGING,
            STATE_SETTLING,
            STATE_HIDDEN,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {}

    /**
     * Peek at the 16:9 ratio keyline of its parent.
     *
     * <p>This can be used as a parameter for {@link #setPeekHeight(int)}. {@link #getPeekHeight()}
     * will return this when the value is set.
     */
    public static final int PEEK_HEIGHT_AUTO = -1;

    /** This flag will preserve the peekHeight int value on configuration change. */
    public static final int SAVE_PEEK_HEIGHT = 0x1;

    /** This flag will preserve the hideable boolean value on configuration change. */
    public static final int SAVE_HIDEABLE = 1 << 2;


    /** This flag will preserve all aforementioned values on configuration change. */
    public static final int SAVE_ALL = -1;

    /**
     * This flag will not preserve the aforementioned values set at runtime if the view is destroyed
     * and recreated. The only value preserved will be the positional state, e.g. collapsed, hidden,
     * expanded, etc. This is the default behavior.
     */
    public static final int SAVE_NONE = 0;

    /** @hide */
    @IntDef(
            flag = true,
            value = {
                    SAVE_PEEK_HEIGHT,
                    SAVE_HIDEABLE,
                    SAVE_ALL,
                    SAVE_NONE,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SaveFlags {}

    private static final String TAG = "BottomSheetBehavior";

    @SaveFlags
    private int saveFlags = SAVE_NONE;

    private static final float HIDE_THRESHOLD = 0.5f;

    private static final float HIDE_FRICTION = 0.1f;

    private static final int CORNER_ANIMATION_DURATION = 500;

    private float maximumVelocity;

    /** Peek height set by the user. */
    private int peekHeight;

    /** Whether or not to use automatic peek height. */
    private boolean peekHeightAuto;

    private SettleRunnable settleRunnable = null;

    int minOffset;

    int maxOffset;

    float elevation = -1;

    boolean hideable;

    @State
    int state = STATE_EXPANDED;

    @Nullable
    ViewDragHelper viewDragHelper;

    private boolean ignoreEvents;

    private int lastNestedScrollDy;

    private boolean nestedScrolled;

    int parentWidth;
    int parentHeight;

    @Nullable
    WeakReference<V> viewRef;

    @Nullable
    WeakReference<View> nestedScrollingChildRef;

    @NonNull private final ArrayList<TopSheetCallback> callbacks = new ArrayList<>();

    @Nullable private VelocityTracker velocityTracker;

    int activePointerId;

    private int initialY;

    boolean touchingScrollingChild;

    @Nullable private Map<View, Integer> importantForAccessibilityMap;

    public TopSheetBehavior() {}

    public TopSheetBehavior(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TopSheetBehavior_Layout);

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            this.elevation = a.getDimension(R.styleable.TopSheetBehavior_Layout_android_elevation, -1);
        }

        TypedValue value = a.peekValue(R.styleable.TopSheetBehavior_Layout_top_behavior_peekHeight);
        if (value != null && value.data == PEEK_HEIGHT_AUTO) {
            setPeekHeight(value.data);
        } else {
            setPeekHeight(
                    a.getDimensionPixelSize(
                            R.styleable.TopSheetBehavior_Layout_top_behavior_peekHeight, PEEK_HEIGHT_AUTO));
        }
        setHideable(a.getBoolean(R.styleable.TopSheetBehavior_Layout_top_behavior_hideable, false));
        setSaveFlags(a.getInt(R.styleable.TopSheetBehavior_Layout_top_behavior_saveFlags, SAVE_NONE));
        a.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @NonNull
    @Override
    public Parcelable onSaveInstanceState(@NonNull CoordinatorLayout parent, @NonNull V child) {
        return new SavedState(super.onSaveInstanceState(parent, child), this);
    }

    @Override
    public void onRestoreInstanceState(
            @NonNull CoordinatorLayout parent, @NonNull V child, @NonNull Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        // Restore Optional State values designated by saveFlags
        restoreOptionalState(ss);
        // Intermediate states are restored as collapsed state
        if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING) {
            this.state = STATE_COLLAPSED;
        } else {
            this.state = ss.state;
        }
    }

    @Override
    public void onAttachedToLayoutParams(@NonNull CoordinatorLayout.LayoutParams layoutParams) {
        super.onAttachedToLayoutParams(layoutParams);
        // These may already be null, but just be safe, explicitly assign them. This lets us know the
        // first time we layout with this behavior by checking (viewRef == null).
        viewRef = null;
        viewDragHelper = null;
    }

    @Override
    public void onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams();
        // Release references so we don't run unnecessary codepaths while not attached to a view.
        viewRef = null;
        viewDragHelper = null;
    }

    @Override
    public boolean onLayoutChild(
            @NonNull CoordinatorLayout parent, @NonNull V child, int layoutDirection) {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.setFitsSystemWindows(true);
        }

        if (viewRef == null) {
            // First layout with this behavior.
            viewRef = new WeakReference<>(child);
            updateAccessibilityActions();
            if (ViewCompat.getImportantForAccessibility(child)
                    == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }

            child.setBackgroundColor(Color.TRANSPARENT);
        }
        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, dragCallback);
        }

        int savedTop = child.getTop();
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection);
        // Offset the top sheet
        parentWidth = parent.getWidth();
        parentHeight = parent.getHeight();

        minOffset = -child.getHeight();
        maxOffset = 0;
        calculateCollapsedOffset();

        if (state == STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, maxOffset);
        } else if (hideable && state == STATE_HIDDEN) {
            ViewCompat.offsetTopAndBottom(child, -child.getHeight());
        } else if (state == STATE_COLLAPSED) {
            ViewCompat.offsetTopAndBottom(child, minOffset);
        } else if (state == STATE_DRAGGING || state == STATE_SETTLING) {
            ViewCompat.offsetTopAndBottom(child, savedTop - child.getTop());
        }

        nestedScrollingChildRef = new WeakReference<>(findScrollingChild(child));
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(
            @NonNull CoordinatorLayout parent, @NonNull V child, @NonNull MotionEvent event) {
        if (!child.isShown()) {
            ignoreEvents = true;
            return false;
        }
        int action = event.getActionMasked();
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchingScrollingChild = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                // Reset the ignore flag
                if (ignoreEvents) {
                    ignoreEvents = false;
                    return false;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                int initialX = (int) event.getX();
                initialY = (int) event.getY();
                // Only intercept nested scrolling events here if the view not being moved by the
                // ViewDragHelper.
                if (state != STATE_SETTLING) {
                    View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
                    if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
                        activePointerId = event.getPointerId(event.getActionIndex());
                        touchingScrollingChild = true;
                    }
                }
                ignoreEvents =
                        activePointerId == MotionEvent.INVALID_POINTER_ID
                                && !parent.isPointInChildBounds(child, initialX, initialY);
                break;
            default: // fall out
        }
        if (!ignoreEvents
                && viewDragHelper != null
                && viewDragHelper.shouldInterceptTouchEvent(event)) {
            return true;
        }
        // We have to handle cases that the ViewDragHelper does not capture the top sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
        return action == MotionEvent.ACTION_MOVE
                && scroll != null
                && !ignoreEvents
                && state != STATE_DRAGGING
                && !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY())
                && viewDragHelper != null
                && Math.abs(initialY - event.getY()) > viewDragHelper.getTouchSlop();
    }

    @Override
    public boolean onTouchEvent(
            @NonNull CoordinatorLayout parent, @NonNull V child, @NonNull MotionEvent event) {
        if (!child.isShown()) {
            return false;
        }
        int action = event.getActionMasked();
        if (state == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (viewDragHelper != null) {
            viewDragHelper.processTouchEvent(event);
        }
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
        // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
        // to capture the top sheet in case it is not captured and the touch slop is passed.
        if (action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
            if (Math.abs(initialY - event.getY()) > viewDragHelper.getTouchSlop()) {
                viewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
            }
        }
        return !ignoreEvents;
    }

    @Override
    public boolean onStartNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View directTargetChild,
            @NonNull View target,
            int axes,
            int type) {
        lastNestedScrollDy = 0;
        nestedScrolled = false;
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedPreScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View target,
            int dx,
            int dy,
            @NonNull int[] consumed,
            int type) {
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            // Ignore fling here. The ViewDragHelper handles it.
            return;
        }
        View scrollingChild = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
        if (target != scrollingChild) {
            return;
        }
        int currentTop = child.getTop();
        int newTop = currentTop - dy;
        if (dy < 0) { // Downward
            if (newTop >= maxOffset) {
                consumed[1] = currentTop - maxOffset;
                ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                setStateInternal(STATE_EXPANDED);
            } else {
                consumed[1] = dy;
                ViewCompat.offsetTopAndBottom(child, -dy);
                setStateInternal(STATE_DRAGGING);
            }
        } else if (dy > 0) { // Upward
            if (!target.canScrollVertically(-1)) {
                if (newTop >= minOffset || hideable) {
                    consumed[1] = dy;
                    ViewCompat.offsetTopAndBottom(child, -dy);
                    setStateInternal(STATE_DRAGGING);
                } else {
                    consumed[1] = currentTop - minOffset;
                    ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                    setStateInternal(STATE_COLLAPSED);
                }
            }
        }
        dispatchOnSlide(child.getTop());
        lastNestedScrollDy = dy;
        nestedScrolled = true;
    }

    @Override
    public void onStopNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View target,
            int type) {
        if (child.getTop() == maxOffset) {
            setStateInternal(STATE_EXPANDED);
            return;
        }
        if (nestedScrollingChildRef == null
                || target != nestedScrollingChildRef.get()
                || !nestedScrolled) {
            return;
        }
        int top;
        int targetState;
        if (lastNestedScrollDy < 0) {
            top = maxOffset;
            targetState = STATE_EXPANDED;
        } else if (hideable && shouldHide(child, getYVelocity())) {
            top = -child.getHeight();
            targetState = STATE_HIDDEN;
        } else if (lastNestedScrollDy == 0) {
            int currentTop = child.getTop();
            if (Math.abs(currentTop - minOffset) > Math.abs(currentTop - maxOffset)) {
                top = maxOffset;
                targetState = STATE_EXPANDED;
            } else {
                top = minOffset;
                targetState = STATE_COLLAPSED;
            }
        } else {
            top = minOffset;
            targetState = STATE_COLLAPSED;
        }
        startSettlingAnimation(child, targetState, top, false);
        nestedScrolled = false;
    }

    @Override
    public void onNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View target,
            int dxConsumed,
            int dyConsumed,
            int dxUnconsumed,
            int dyUnconsumed,
            int type,
            @NonNull int[] consumed) {
        // Overridden to prevent the default consumption of the entire scroll distance.
    }

    @Override
    public boolean onNestedPreFling(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View target,
            float velocityX,
            float velocityY) {
        if (nestedScrollingChildRef != null) {
            return target == nestedScrollingChildRef.get()
                    && (state != STATE_EXPANDED
                    || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY));
        } else {
            return false;
        }
    }

    /**
     * Sets the height of the top sheet when it is collapsed.
     *
     * @param peekHeight The height of the collapsed top sheet in pixels, or {@link
     *     #PEEK_HEIGHT_AUTO} to configure the sheet to peek automatically at 16:9 ratio keyline.
     * @attr ref
     *     com.google.android.material.R.styleable#TopSheetBehavior_Layout_behavior_peekHeight
     */
    public void setPeekHeight(int peekHeight) {
        setPeekHeight(peekHeight, false);
    }

    /**
     * Sets the height of the top sheet when it is collapsed while optionally animating between the
     * old height and the new height.
     *
     * @param peekHeight The height of the collapsed top sheet in pixels, or {@link
     *     #PEEK_HEIGHT_AUTO} to configure the sheet to peek automatically at 16:9 ratio keyline.
     * @param animate Whether to animate between the old height and the new height.
     * @attr ref
     *     com.google.android.material.R.styleable#TopSheetBehavior_Layout_behavior_peekHeight
     */
    public final void setPeekHeight(int peekHeight, boolean animate) {
        boolean layout = false;
        if (peekHeight == PEEK_HEIGHT_AUTO) {
            if (!peekHeightAuto) {
                peekHeightAuto = true;
                layout = true;
            }
        } else if (peekHeightAuto || this.peekHeight != peekHeight) {
            peekHeightAuto = false;
            this.peekHeight = Math.max(0, peekHeight);
            layout = true;
        }
        // If sheet is already laid out, recalculate the collapsed offset based on new setting.
        // Otherwise, let onLayoutChild handle this later.
        if (layout && viewRef != null) {
            calculateCollapsedOffset();
            if (state == STATE_COLLAPSED) {
                V view = viewRef.get();
                if (view != null) {
                    if (animate) {
                        settleToStatePendingLayout(state);
                    } else {
                        view.requestLayout();
                    }
                }
            }
        }
    }

    /**
     * Gets the height of the top sheet when it is collapsed.
     *
     * @return The height of the collapsed top sheet in pixels, or {@link #PEEK_HEIGHT_AUTO} if the
     *     sheet is configured to peek automatically at 16:9 ratio keyline
     * @attr ref
     *     com.google.android.material.R.styleable#TopSheetBehavior_Layout_behavior_peekHeight
     */
    public int getPeekHeight() {
        return peekHeightAuto ? PEEK_HEIGHT_AUTO : peekHeight;
    }

    /**
     * Sets whether this top sheet can hide when it is swiped down.
     *
     * @param hideable {@code true} to make this top sheet hideable.
     * @attr ref com.google.android.material.R.styleable#TopSheetBehavior_Layout_behavior_hideable
     */
    public void setHideable(boolean hideable) {
        if (this.hideable != hideable) {
            this.hideable = hideable;
            if (!hideable && state == STATE_HIDDEN) {
                // Lift up to collapsed state
                setState(STATE_COLLAPSED);
            }
            updateAccessibilityActions();
        }
    }

    /**
     * Gets whether this top sheet can hide when it is swiped down.
     *
     * @return {@code true} if this top sheet can hide.
     * @attr ref com.google.android.material.R.styleable#TopSheetBehavior_Layout_behavior_hideable
     */
    public boolean isHideable() {
        return hideable;
    }

    /**
     * Sets save flags to be preserved in bottomsheet on configuration change.
     *
     * @param flags bitwise int of {@link #SAVE_PEEK_HEIGHT}, {@link
     *     #SAVE_HIDEABLE}, {@link #SAVE_ALL} and {@link #SAVE_NONE}.
     * @see #getSaveFlags()
     * @attr ref com.google.android.material.R.styleable#TopSheetBehavior_Layout_behavior_saveFlags
     */
    public void setSaveFlags(@SaveFlags int flags) {
        this.saveFlags = flags;
    }
    /**
     * Returns the save flags.
     *
     * @see #setSaveFlags(int)
     * @attr ref com.google.android.material.R.styleable#TopSheetBehavior_Layout_behavior_saveFlags
     */
    @SaveFlags
    public int getSaveFlags() {
        return this.saveFlags;
    }

    /**
     * Sets a callback to be notified of top sheet events.
     *
     * @param callback The callback to notify when top sheet events occur.
     * @deprecated use {@link #addTopSheetCallback(TopSheetCallback)} and {@link
     *     #removeTopSheetCallback(TopSheetCallback)} instead
     */
    @Deprecated
    public void setBottomSheetCallback(TopSheetCallback callback) {
        Log.w(
                TAG,
                "BottomSheetBehavior now supports multiple callbacks. `setBottomSheetCallback()` removes"
                        + " all existing callbacks, including ones set internally by library authors, which"
                        + " may result in unintended behavior. This may change in the future. Please use"
                        + " `addBottomSheetCallback()` and `removeBottomSheetCallback()` instead to set your"
                        + " own callbacks.");
        callbacks.clear();
        if (callback != null) {
            callbacks.add(callback);
        }
    }

    /**
     * Adds a callback to be notified of top sheet events.
     *
     * @param callback The callback to notify when top sheet events occur.
     */
    public void addTopSheetCallback(@NonNull TopSheetCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    /**
     * Removes a previously added callback.
     *
     * @param callback The callback to remove.
     */
    public void removeTopSheetCallback(@NonNull TopSheetCallback callback) {
        callbacks.remove(callback);
    }

    /**
     * Sets the state of the top sheet. The top sheet will transition to that state with
     * animation.
     *
     * @param state One of {@link #STATE_COLLAPSED}, {@link #STATE_EXPANDED}, {@link #STATE_HIDDEN}.
     */
    public void setState(@State int state) {
        if (state == this.state) {
            return;
        }
        if (viewRef == null) {
            // The view is not laid out yet; modify mState and let onLayoutChild handle it later
            if (state == STATE_COLLAPSED
                    || state == STATE_EXPANDED
                    || (hideable && state == STATE_HIDDEN)) {
                this.state = state;
            }
            return;
        }
        settleToStatePendingLayout(state);
    }

    private void settleToStatePendingLayout(@State int state) {
        final V child = null != viewRef ? viewRef.get() : null;
        if (child == null) {
            return;
        }
        // Start the animation; wait until a pending layout if there is one.
        ViewParent parent = child.getParent();
        if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
            final int finalState = state;
            child.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            settleToState(child, finalState);
                        }
                    });
        } else {
            settleToState(child, state);
        }
    }

    /**
     * Gets the current state of the top sheet.
     *
     * @return One of {@link #STATE_EXPANDED},{@link #STATE_COLLAPSED},
     *     {@link #STATE_DRAGGING}, {@link #STATE_SETTLING}.
     */
    @State
    public int getState() {
        return state;
    }

    void setStateInternal(@State int state) {
        if (this.state == state) {
            return;
        }
        this.state = state;

        if (viewRef == null) {
            return;
        }

        View bottomSheet = viewRef.get();
        if (bottomSheet == null) {
            return;
        }

        if (state == STATE_EXPANDED) {
            updateImportantForAccessibility(true);
        } else if (state == STATE_HIDDEN || state == STATE_COLLAPSED) {
            updateImportantForAccessibility(false);
        }

        updateDrawableForTargetState(state);
        for (int i = 0; i < callbacks.size(); i++) {
            callbacks.get(i).onStateChanged(bottomSheet, state);
        }
        updateAccessibilityActions();
    }

    private void updateDrawableForTargetState(@State int state) {
        if (state == STATE_SETTLING) {
            // Special case: we want to know which state we're settling to, so wait for another call.
            return;
        }

        boolean expand = state == STATE_EXPANDED;
    }

    private void calculateCollapsedOffset() {
        View child = null != viewRef ? viewRef.get() : null;
        if (null != child) {
            minOffset = -child.getHeight();
        }
    }

    private void reset() {
        activePointerId = ViewDragHelper.INVALID_POINTER;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private void restoreOptionalState(@NonNull SavedState ss) {
        if (this.saveFlags == SAVE_NONE) {
            return;
        }
        if (this.saveFlags == SAVE_ALL || (this.saveFlags & SAVE_PEEK_HEIGHT) == SAVE_PEEK_HEIGHT) {
            this.peekHeight = ss.peekHeight;
        }
        if (this.saveFlags == SAVE_ALL || (this.saveFlags & SAVE_HIDEABLE) == SAVE_HIDEABLE) {
            this.hideable = ss.hideable;
        }
    }

    boolean shouldHide(@NonNull View child, float yvel) {
        if (child.getTop() > maxOffset) {
            // It should not hide, but collapse.
            return false;
        }
        final float newTop = child.getTop() + yvel * HIDE_FRICTION;
        peekHeight = peekHeight <= 0 ? child.getHeight() : peekHeight;
        return Math.abs(newTop - maxOffset) / (float) peekHeight > HIDE_THRESHOLD;
    }

    @Nullable
    @VisibleForTesting
    View findScrollingChild(View view) {
        if (ViewCompat.isNestedScrollingEnabled(view)) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }
        return null;
    }

    private float getYVelocity() {
        if (velocityTracker == null) {
            return 0;
        }
        velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
        return velocityTracker.getYVelocity(activePointerId);
    }

    void settleToState(@NonNull View child, int state) {
        int top;
        if (state == STATE_COLLAPSED) {
            top = minOffset;
        } else if (state == STATE_EXPANDED) {
            top = maxOffset;
        } else if (hideable && state == STATE_HIDDEN) {
            top = parentHeight;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        startSettlingAnimation(child, state, top, false);
    }

    void startSettlingAnimation(View child, int state, int top, boolean settleFromViewDragHelper) {
        boolean startedSettling =
                settleFromViewDragHelper
                        ? viewDragHelper.settleCapturedViewAt(child.getLeft(), top)
                        : viewDragHelper.smoothSlideViewTo(child, child.getLeft(), top);
        if (startedSettling) {
            setStateInternal(STATE_SETTLING);
            // STATE_SETTLING won't animate the material shape, so do that here with the target state.
            updateDrawableForTargetState(state);
            if (settleRunnable == null) {
                // If the singleton SettleRunnable instance has not been instantiated, create it.
                settleRunnable = new SettleRunnable(child, state);
            }
            // If the SettleRunnable has not been posted, post it with the correct state.
            if (settleRunnable.isPosted == false) {
                settleRunnable.targetState = state;
                ViewCompat.postOnAnimation(child, settleRunnable);
                settleRunnable.isPosted = true;
            } else {
                // Otherwise, if it has been posted, just update the target state.
                settleRunnable.targetState = state;
            }
        } else {
            setStateInternal(state);
        }
    }

    private final ViewDragHelper.Callback dragCallback =
            new ViewDragHelper.Callback() {

                @Override
                public boolean tryCaptureView(@NonNull View child, int pointerId) {
                    if (state == STATE_DRAGGING) {
                        return false;
                    }
                    if (touchingScrollingChild) {
                        return false;
                    }
                    if (state == STATE_EXPANDED && activePointerId == pointerId) {
                        View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
                        if (scroll != null && scroll.canScrollVertically(-1)) {
                            // Let the content scroll up
                            return false;
                        }
                    }
                    return viewRef != null && viewRef.get() == child;
                }

                @Override
                public void onViewPositionChanged(
                        @NonNull View changedView, int left, int top, int dx, int dy) {
                    dispatchOnSlide(top);
                }

                @Override
                public void onViewDragStateChanged(int state) {
                    if (state == ViewDragHelper.STATE_DRAGGING) {
                        setStateInternal(STATE_DRAGGING);
                    }
                }

                @Override
                public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
                    // xvel 水平方向速度
                    // yvel 垂直方向速度
                    int top;
                    @State int targetState;
                    if (yvel > 0) { // Moving up
                        top = maxOffset;
                        targetState = STATE_EXPANDED;
                    } else if (hideable
                            && shouldHide(releasedChild, yvel)
                            && (releasedChild.getTop() < maxOffset || Math.abs(xvel) < Math.abs(yvel))) {
                        // Hide if we shouldn't collapse and the view was either released low or it was a
                        // vertical swipe.
                        top = minOffset;
                        targetState = STATE_HIDDEN;
                    } else if (yvel == 0.f || Math.abs(xvel) > Math.abs(yvel)) {
                        // If the Y velocity is 0 or the swipe was mostly horizontal indicated by the X velocity
                        // being greater than the Y velocity, settle to the nearest correct height.
                        int currentTop = releasedChild.getTop();
                        if (Math.abs(currentTop - minOffset) > Math.abs(currentTop - maxOffset)) {
                            top = maxOffset;
                            targetState = STATE_EXPANDED;
                        } else {
                            top = minOffset;
                            targetState = STATE_COLLAPSED;
                        }
                    } else { // Moving Down
                        top = minOffset;
                        targetState = STATE_COLLAPSED;
                    }
                    startSettlingAnimation(releasedChild, targetState, top, true);
                }

                @Override
                public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
                    // child 操作的view
                    // top 将要到达的位置
                    // 相对与当前位置的偏移
                    // 所处的垂直位置
                    return MathUtils.clamp(
                            top, hideable ? -child.getHeight() : maxOffset, maxOffset);
                }

                @Override
                public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
                    return child.getLeft();
                }

                @Override
                public int getViewVerticalDragRange(@NonNull View child) {
                    if (hideable) {
                        return child.getHeight();
                    } else {
                        return maxOffset - minOffset;
                    }
                }
            };

    void dispatchOnSlide(int top) {
        View bottomSheet = viewRef.get();
        if (bottomSheet != null && !callbacks.isEmpty()) {
            peekHeight = bottomSheet.getHeight();
            float slideOffset =
                    (top < minOffset)
                            ? (float) (top - minOffset) / peekHeight
                            : (float) (top - minOffset) / (maxOffset - minOffset);
            for (int i = 0; i < callbacks.size(); i++) {
                callbacks.get(i).onSlide(bottomSheet, slideOffset);
            }
        }
    }

    private class SettleRunnable implements Runnable {

        private final View view;

        private boolean isPosted;

        @State
        int targetState;

        SettleRunnable(View view, @State int targetState) {
            this.view = view;
            this.targetState = targetState;
        }

        @Override
        public void run() {
            if (viewDragHelper != null && viewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(view, this);
            } else {
                setStateInternal(targetState);
            }
            this.isPosted = false;
        }
    }

    /** State persisted across instances */
    protected static class SavedState extends AbsSavedState {
        @State
        final int state;
        int peekHeight;
        boolean fitToContents;
        boolean hideable;
        boolean skipCollapsed;

        public SavedState(@NonNull Parcel source) {
            this(source, null);
        }

        public SavedState(@NonNull Parcel source, ClassLoader loader) {
            super(source, loader);
            //noinspection ResourceType
            state = source.readInt();
            peekHeight = source.readInt();
            fitToContents = source.readInt() == 1;
            hideable = source.readInt() == 1;
            skipCollapsed = source.readInt() == 1;
        }

        public SavedState(Parcelable superState, @NonNull TopSheetBehavior<?> behavior) {
            super(superState);
            this.state = behavior.state;
            this.peekHeight = behavior.peekHeight;
            this.hideable = behavior.hideable;
        }

        /**
         * This constructor does not respect flags: {@link TopSheetBehavior#SAVE_PEEK_HEIGHT}, {@link
         * {@link TopSheetBehavior#SAVE_HIDEABLE}. It is as if {@link TopSheetBehavior#SAVE_NONE}
         * were set.
         *
         * @deprecated Use {@link SavedState (Parcelable, TopSheetBehavior )} instead.
         */
        @Deprecated
        public SavedState(Parcelable superstate, int state) {
            super(superstate);
            this.state = state;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(state);
            out.writeInt(peekHeight);
            out.writeInt(fitToContents ? 1 : 0);
            out.writeInt(hideable ? 1 : 0);
            out.writeInt(skipCollapsed ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR =
                new ClassLoaderCreator<SavedState>() {
                    @NonNull
                    @Override
                    public SavedState createFromParcel(@NonNull Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Nullable
                    @Override
                    public SavedState createFromParcel(@NonNull Parcel in) {
                        return new SavedState(in, null);
                    }

                    @NonNull
                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    /**
     * A utility function to get the {@link TopSheetBehavior} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link TopSheetBehavior}.
     * @return The {@link TopSheetBehavior} associated with the {@code view}.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <V extends View> TopSheetBehavior<V> from(@NonNull V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior<?> behavior =
                ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (!(behavior instanceof TopSheetBehavior)) {
            throw new IllegalArgumentException("The view is not associated with TopSheetBehavior");
        }
        return (TopSheetBehavior<V>) behavior;
    }

    private void updateImportantForAccessibility(boolean expanded) {
        if (viewRef == null) {
            return;
        }

        ViewParent viewParent = viewRef.get().getParent();
        if (!(viewParent instanceof CoordinatorLayout)) {
            return;
        }

        CoordinatorLayout parent = (CoordinatorLayout) viewParent;
        final int childCount = parent.getChildCount();
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) && expanded) {
            if (importantForAccessibilityMap == null) {
                importantForAccessibilityMap = new HashMap<>(childCount);
            } else {
                // The important for accessibility values of the child views have been saved already.
                return;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            if (child == viewRef.get()) {
                continue;
            }

            if (!expanded) {
                if (importantForAccessibilityMap != null
                        && importantForAccessibilityMap.containsKey(child)) {
                    // Restores the original important for accessibility value of the child view.
                    ViewCompat.setImportantForAccessibility(child, importantForAccessibilityMap.get(child));
                }
            } else {
                // Saves the important for accessibility value of the child view.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    importantForAccessibilityMap.put(child, child.getImportantForAccessibility());
                }

                ViewCompat.setImportantForAccessibility(
                        child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
        }

        if (!expanded) {
            importantForAccessibilityMap = null;
        }
    }

    private void updateAccessibilityActions() {
        if (viewRef == null) {
            return;
        }
        V child = viewRef.get();
        if (child == null) {
            return;
        }
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_COLLAPSE);
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_EXPAND);
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_DISMISS);

        if (hideable && state != STATE_HIDDEN) {
            addAccessibilityActionForState(child, AccessibilityActionCompat.ACTION_DISMISS, STATE_HIDDEN);
        }

        switch (state) {
            case STATE_EXPANDED:
            {
                int nextState = STATE_COLLAPSED;
                addAccessibilityActionForState(
                        child, AccessibilityActionCompat.ACTION_COLLAPSE, nextState);
                break;
            }
            case STATE_COLLAPSED:
            {
                int nextState = STATE_EXPANDED;
                addAccessibilityActionForState(child, AccessibilityActionCompat.ACTION_EXPAND, nextState);
                break;
            }
            default: // fall out
        }
    }

    private void addAccessibilityActionForState(
            V child, AccessibilityActionCompat action, final int state) {
        ViewCompat.replaceAccessibilityAction(
                child,
                action,
                null,
                new AccessibilityViewCommand() {
                    @Override
                    public boolean perform(@NonNull View view, @Nullable CommandArguments arguments) {
                        setState(state);
                        return true;
                    }
                });
    }
}


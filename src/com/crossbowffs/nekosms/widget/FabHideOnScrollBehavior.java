package com.crossbowffs.nekosms.widget;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

public class FabHideOnScrollBehavior extends FloatingActionButton.Behavior {
    private boolean mAutoHide;

    public FabHideOnScrollBehavior() {
        super();
    }

    public FabHideOnScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAutoHideEnabled(boolean enabled) {
        mAutoHide = enabled;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child,
                                       View directTargetChild, View target, int nestedScrollAxes) {
        return mAutoHide && nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child, View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
            child.hide();
        } else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            child.show();
        }
    }
}

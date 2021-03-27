package sae.iit.saedashboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

public class CustomSwipeViewPager extends ViewPager {

    private boolean lock = false;

    public boolean isLocked() {
        return lock;
    }

    public void setLocked(boolean lock) {
        this.lock = lock;
    }

    public CustomSwipeViewPager(@NonNull Context context) {
        super(context);
    }

    public CustomSwipeViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return super.onInterceptTouchEvent(event) && !lock;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        performClick();
        return super.onTouchEvent(event) && !lock;
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        return super.onDragEvent(event) && !lock;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.absurd.refreshandloadlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

public class RefreshAndLoadingLayout extends ViewGroup {
    private static final String LOG_TAG = RefreshAndLoadingLayout.class.getSimpleName();

    private static final long RETURN_TO_ORIGINAL_POSITION_TIMEOUT = 300;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final float MAX_SWIPE_DISTANCE_FACTOR = .6f;
    private static final int REFRESH_TRIGGER_DISTANCE = 120;
    private static final int INVALID_POINTER = -1;

    private View mTarget; // the content that gets pulled down
    private int mOriginalOffsetTop;
    private OnRefreshListener mListener;
    private int mFrom;
    private boolean mRefreshing = false;
    private int mTouchSlop;
    private float mHeaderDistanceToTriggerSync = -1;
    private float mBooterDistanceToTriggerSync = -1;
    private int mMediumAnimationDuration;
    private int mCurrentTargetOffsetTop;

    private float mInitialMotionY;
    private float mLastMotionY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{android.R.attr.enabled};

    private View mHeaderView, mBooterView;
    private int mHeaderHeight, mBooterHeight;
    private STATUS mStatus = STATUS.NORMAL;
    private boolean mDisable; // 用来控制控件是否允许滚动
    private boolean mCurrentIsHeaderrefresh = true;

    private enum STATUS {
        NORMAL, LOOSEN, REFRESHING
    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int offset = 0;
            if (mCurrentIsHeaderrefresh == true) {
                if (mFrom != mOriginalOffsetTop) {
                    targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
                }
                offset = targetTop - mTarget.getTop();
                final int currentTop = mTarget.getTop();
                if (offset + currentTop < 0) {
                    offset = 0 - currentTop;
                }
            }else {
                if (mFrom != mOriginalOffsetTop) {
                    targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
                }
                offset = targetTop - mTarget.getTop();
                final int currentTop = mTarget.getTop();
                if (offset + currentTop > 0) {
                    offset = currentTop;
                }
            }
            setTargetOffsetTopAndBottom(offset, mCurrentIsHeaderrefresh);
        }
    };

    private final Animation mAnimateToHeaderPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int offset = 0;
            if (mCurrentIsHeaderrefresh == true) {
                if (mFrom != mHeaderHeight) {
                    targetTop = (mFrom + (int) ((mHeaderHeight - mFrom) * interpolatedTime));
                }
                offset = targetTop - mTarget.getTop();
                final int currentTop = mTarget.getTop();
                if (offset + currentTop < 0) {
                    offset = 0 - currentTop;
                }
            } else {
                if (mFrom != -mBooterHeight) {
                    targetTop = (mFrom + (int) ((-mBooterHeight - mFrom) * interpolatedTime));
                }
                offset = targetTop - mTarget.getTop();
                final int currentTop = mTarget.getTop();
                if (offset + currentTop > 0) {
                    offset = currentTop;
                }
            }
            Log.v("TAG", "---offset2--->>" + offset);
            setTargetOffsetTopAndBottom(offset, mCurrentIsHeaderrefresh);
        }
    };

    private final AnimationListener mReturnToStartPositionListener = new BaseAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            mCurrentTargetOffsetTop = 0;
            mStatus = STATUS.NORMAL;
            mDisable = false;
        }
    };

    private final AnimationListener mReturnToHeaderPositionListener = new BaseAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            // Once the target content has returned to its start position, reset
            // the target offset to 0
            mCurrentTargetOffsetTop = mHeaderHeight;
            mStatus = STATUS.REFRESHING;
        }
    };

    private final Runnable mReturnToStartPosition = new Runnable() {
        @Override
        public void run() {
            mReturningToStart = true;
            animateOffsetToStartPosition(mCurrentTargetOffsetTop + getPaddingTop(), mReturnToStartPositionListener);
        }
    };

    private final Runnable mReturnToHeaderPosition = new Runnable() {
        @Override
        public void run() {
            mReturningToStart = true;
            animateOffsetToHeaderPosition(mCurrentTargetOffsetTop + getPaddingTop(), mReturnToHeaderPositionListener);
        }
    };

    // Cancel the refresh gesture and animate everything back to its original
    // state.
    private final Runnable mCancel = new Runnable() {
        @Override
        public void run() {
            mReturningToStart = true;
            animateOffsetToStartPosition(mCurrentTargetOffsetTop + getPaddingTop(), mReturnToStartPositionListener);
        }
    };

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public RefreshAndLoadingLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public RefreshAndLoadingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        mDecelerateInterpolator = new DecelerateInterpolator(
                DECELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(mCancel);
        removeCallbacks(mReturnToStartPosition);
        removeCallbacks(mReturnToHeaderPosition);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mReturnToStartPosition);
        removeCallbacks(mCancel);
        removeCallbacks(mReturnToHeaderPosition);
    }

    private void animateOffsetToStartPosition(int from, AnimationListener listener) {
        mFrom = from;
        mAnimateToStartPosition.reset();
        mAnimateToStartPosition.setDuration(mMediumAnimationDuration);
        mAnimateToStartPosition.setAnimationListener(listener);
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
        mTarget.startAnimation(mAnimateToStartPosition);
    }

    private void animateOffsetToHeaderPosition(int from, AnimationListener listener) {
        mFrom = from;
        mAnimateToHeaderPosition.reset();
        mAnimateToHeaderPosition.setDuration(mMediumAnimationDuration);
        mAnimateToHeaderPosition.setAnimationListener(listener);
        mAnimateToHeaderPosition.setInterpolator(mDecelerateInterpolator);
        mTarget.startAnimation(mAnimateToHeaderPosition);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (mRefreshing != refreshing) {
            ensureTarget();
            mRefreshing = refreshing;
        }
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     * progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            if (getChildCount() > 3 && !isInEditMode()) {
                throw new IllegalStateException("SwipeRefreshLayout can only host Three children");
            }
            mTarget = getChildAt(1);

            // 控制是否允许滚动
            mTarget.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return mDisable;
                }
            });

            mOriginalOffsetTop = mTarget.getTop() + getPaddingTop();
        }
        if (mHeaderDistanceToTriggerSync == -1) {
            if (getParent() != null && ((View) getParent()).getHeight() > 0) {
                final DisplayMetrics metrics = getResources().getDisplayMetrics();
                mHeaderDistanceToTriggerSync = (int) Math.min(((View) getParent()).getHeight() * MAX_SWIPE_DISTANCE_FACTOR, REFRESH_TRIGGER_DISTANCE * metrics.density);
            }
        }
        if (mBooterDistanceToTriggerSync == -1) {
            if (getParent() != null && ((View) getParent()).getHeight() > 0) {
                final DisplayMetrics metrics = getResources().getDisplayMetrics();
                mBooterDistanceToTriggerSync = (int) Math.min(((View) getParent()).getHeight() * MAX_SWIPE_DISTANCE_FACTOR, REFRESH_TRIGGER_DISTANCE * metrics.density);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0 || getChildCount() == 1) {
            return;
        }
        final View child = getChildAt(1);
        final int childLeft = getPaddingLeft();
        final int childTop = mCurrentTargetOffsetTop + getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        mHeaderView.layout(childLeft, childTop - mHeaderHeight, childLeft + childWidth, childTop);
        mBooterView.layout(childLeft, child.getMeasuredHeight(), childLeft + childWidth, child.getMeasuredHeight() + mBooterHeight);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (getChildCount() <= 2) {
            throw new IllegalStateException("SwipeRefreshLayout must have the headerview and contentview");
        }

        if (getChildCount() > 3 && !isInEditMode()) {
            throw new IllegalStateException("SwipeRefreshLayout can only host two children");
        }

        if (mHeaderView == null) {
            mHeaderView = getChildAt(0);
            measureChild(mHeaderView, widthMeasureSpec, heightMeasureSpec);
            mHeaderHeight = mHeaderView.getMeasuredHeight();
            mHeaderDistanceToTriggerSync = mHeaderHeight;
        }

        if (mBooterView == null) {
            mBooterView = getChildAt(2);
            measureChild(mBooterView, widthMeasureSpec, heightMeasureSpec);
            mBooterHeight = mBooterView.getMeasuredHeight();
            mBooterDistanceToTriggerSync = mBooterHeight;
        }

        getChildAt(1).measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight()
                                - getPaddingTop() - getPaddingBottom(),
                        MeasureSpec.EXACTLY));
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0 && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
            } else {
                return mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    public boolean canChildScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                //     return absListView.getChildCount() > 0 && (absListView.getLastVisiblePosition() < absListView.getMeasuredHeight() - absListView.getPaddingTop() || absListView.getChildAt(absListView.getChildCount()).getTop() < absListView.getMeasuredHeight());
                return absListView.getLastVisiblePosition() == absListView.getCount() - 1;
            } else {
                return mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        //   Log.v("TAG","----Down---->>"+ canChildScrollDown()+"   ----Up---->>"+canChildScrollUp());
        if (!isEnabled() || mReturningToStart || canChildScrollUp() || canChildScrollDown() || mStatus == STATUS.REFRESHING) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - mInitialMotionY;
                if (Math.abs(yDiff) > mTouchSlop) {
                    mLastMotionY = y;
                    mIsBeingDragged = true;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp() || canChildScrollDown() || mStatus == STATUS.REFRESHING) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);

                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);

                final float yDiff = y - mInitialMotionY;
                Log.v("TAG", "------>>" + yDiff);
                if (!mIsBeingDragged && Math.abs(yDiff) > mTouchSlop) {
                    mIsBeingDragged = true;
                }

                if (mIsBeingDragged) {
                    // User velocity passed min velocity; trigger a refresh

                    if (Math.abs(yDiff) > mHeaderDistanceToTriggerSync) {
                        if (mStatus == STATUS.NORMAL) {
                            mStatus = STATUS.LOOSEN;

                            if (mListener != null) {
                                mListener.onLoose(mCurrentIsHeaderrefresh);
                            }
                        }

                        updateContentOffsetTop((int) (yDiff), yDiff > 0 ? true : false);
                    } else {
                        if (mStatus == STATUS.LOOSEN) {
                            mStatus = STATUS.NORMAL;

                            if (mListener != null) {
                                mListener.onNormal(mCurrentIsHeaderrefresh);
                            }
                        }

                        updateContentOffsetTop((int) (yDiff), yDiff > 0 ? true : false);
                        if (mLastMotionY > y && mTarget.getTop() == getPaddingTop()) {
                            removeCallbacks(mCancel);
                        }
                    }

                    mLastMotionY = y;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionY = MotionEventCompat.getY(ev, index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
                if (mStatus == STATUS.LOOSEN) {
                    startRefresh();
                } else {
                    updatePositionTimeout();
                }

                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                return false;
            case MotionEvent.ACTION_CANCEL:
                updatePositionTimeout();

                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                return false;
        }

        return true;
    }

    private void startRefresh() {
        removeCallbacks(mCancel);
        mReturnToHeaderPosition.run();
        setRefreshing(true);
        mDisable = true;

        if (mListener != null) {
            mListener.onRefresh(mCurrentIsHeaderrefresh);
        }
    }

    public void stopRefresh() {
        mReturnToStartPosition.run();
    }

    private void updateContentOffsetTop(int targetTop, boolean isHeader) {
        if (isHeader == true) {
            mCurrentIsHeaderrefresh = true;
            final int currentTop = mTarget.getTop();
            if (targetTop > mHeaderDistanceToTriggerSync) {
                targetTop = (int) mHeaderDistanceToTriggerSync + (int) (targetTop - mHeaderDistanceToTriggerSync) / 2; // 超过触发松手刷新的距离后，就只显示滑动一半的距离，避免随手势拉动到最底部，用户体验不好
            } else if (targetTop < 0) {
                targetTop = 0;
            }
            setTargetOffsetTopAndBottom(targetTop - currentTop, true);
        } else {
            mCurrentIsHeaderrefresh = false;
            final int currentTop = mTarget.getTop();
            if (Math.abs(targetTop) > mBooterDistanceToTriggerSync) {
                targetTop = (int) -mBooterDistanceToTriggerSync + (int) (targetTop + mBooterDistanceToTriggerSync) / 2; // 超过触发松手刷新的距离后，就只显示滑动一半的距离，避免随手势拉动到最底部，用户体验不好
            } else if (targetTop > mTarget.getMeasuredHeight()) {
                targetTop = 0;
            }
            setTargetOffsetTopAndBottom(targetTop - currentTop, false);
        }
    }

    private void setTargetOffsetTopAndBottom(int offset, boolean isHeader) {
        if (isHeader == true) {
            mTarget.offsetTopAndBottom(offset);
            mHeaderView.offsetTopAndBottom(offset);
            mCurrentTargetOffsetTop = mTarget.getTop();
            invalidate();
        } else {
            Log.v("TAG", "---isHeader---->>" + offset);
            mTarget.offsetTopAndBottom(offset);
            mBooterView.offsetTopAndBottom(offset);
            mCurrentTargetOffsetTop = mTarget.getTop();
            invalidate();
        }
    }

    private void updatePositionTimeout() {
        removeCallbacks(mCancel);
        postDelayed(mCancel, RETURN_TO_ORIGINAL_POSITION_TIMEOUT);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = MotionEventCompat.getY(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev,
                    newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a normal/ready-refresh/refresh should implement this interface.
     */
    public interface OnRefreshListener {
        public void onNormal(boolean mCurrentIsHeader);

        public void onLoose(boolean mCurrentIsHeade);

        public void onRefresh(boolean mCurrentIsHeade);
    }

    /**
     * Simple AnimationListener to avoid having to implement unneeded methods in
     * AnimationListeners.
     */
    private class BaseAnimationListener implements AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}

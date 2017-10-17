

package com.absurd.refreshandloadlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
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


    private static final long RETURN_TO_ORIGINAL_POSITION_TIMEOUT = 300;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final float MAX_SWIPE_DISTANCE_FACTOR = .6f;
    private static final int REFRESH_TRIGGER_DISTANCE = 120;
    private static final int INVALID_POINTER = -1;

    private View mTarget;
    private int mOriginalOffsetTop;
    private OnRefreshAndLoadListener mListener;
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


    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{android.R.attr.enabled};

    private View mHeaderView, mBooterView;
    private int mHeaderHeight, mBooterHeight;
    private STATUS mStatus = STATUS.NORMAL;
    private boolean mDisable; // 用来控制控件是否允许滚动
    private boolean mCurrentIsHeaderrefresh = true;
    private boolean mRefrshEnabled = true;
    private boolean mLoadEnabled = true;

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
            } else {
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


    private final Runnable mCancel = new Runnable() {
        @Override
        public void run() {
            mReturningToStart = true;
            animateOffsetToStartPosition(mCurrentTargetOffsetTop + getPaddingTop(), mReturnToStartPositionListener);
        }
    };


    public RefreshAndLoadingLayout(Context context) {
        this(context, null);
    }


    public RefreshAndLoadingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshAndLoadingLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMediumAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RefreshAndLoadingLayout, defStyleAttr, 0);
        mRefrshEnabled = ta.getBoolean(R.styleable.RefreshAndLoadingLayout_refreshEnabled, true);
        mLoadEnabled = ta.getBoolean(R.styleable.RefreshAndLoadingLayout_loadEnabled, true);
        ta.recycle();

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


    public void setOnRefreshListener(OnRefreshAndLoadListener listener) {
        mListener = listener;
    }


    public void setRefreshing(boolean refreshing) {
        if (mRefreshing != refreshing) {
            ensureTarget();
            mRefreshing = refreshing;
        }
    }

    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        if (mTarget == null) {
            if (getChildCount() > 3 && !isInEditMode()) {
                throw new IllegalStateException("RefreshAndLoadLayout can only host three children");
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
        final int childTop =   getPaddingTop();
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
            throw new IllegalStateException("RefreshAndLoadLayout can only host three children");
        }

        if (getChildCount() > 3 && !isInEditMode()) {
            throw new IllegalStateException("RefreshAndLoadLayout can only host three children");
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
                return absListView.getChildCount() > 0 && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
            } else {
                return mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, 1);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }
        if (mRefreshing) {
            return false;
        }
        if (!isEnabled() || mReturningToStart || canChildScrollUp() || canChildScrollDown() || mStatus == STATUS.REFRESHING) {
            if (canChildScrollUp() && canChildScrollDown())
                return false;
        }


        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (canChildScrollDown()) {
                    if (ev.getY() - mLastMotionY < 0)
                        return false;
                }
                if (canChildScrollUp()) {
                    if (ev.getY() - mLastMotionY > 0)
                        return false;
                }

                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
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

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!mRefrshEnabled && !mLoadEnabled) {
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
                    return false;
                }

                final float y = ev.getY();

                final float yDiff = y - mInitialMotionY;

                if (!mRefrshEnabled && yDiff > 0) {
                    return false;
                }
                if (!mLoadEnabled && yDiff < 0) {
                    return false;
                }

                if (!mIsBeingDragged && Math.abs(yDiff) > mTouchSlop) {
                    mIsBeingDragged = true;
                }

                if (mIsBeingDragged) {

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
            mHeaderView.offsetTopAndBottom(offset);
            mTarget.offsetTopAndBottom(offset);
            mCurrentTargetOffsetTop = mTarget.getTop();
            invalidate();
        } else {
            mBooterView.offsetTopAndBottom(offset);
            mTarget.offsetTopAndBottom(offset);
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
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = MotionEventCompat.getY(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev,
                    newPointerIndex);
        }
    }


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

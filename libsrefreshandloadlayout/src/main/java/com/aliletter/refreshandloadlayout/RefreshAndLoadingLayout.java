

package com.aliletter.refreshandloadlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
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


public class RefreshAndLoadingLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private View mTarget;
    private OnRefreshAndLoadListener mListener;
    private boolean mRefreshing = false;
    private int mTouchSlop;
    private int mTopDistanceToTriggerSync = -1;
    private int mBottomDistanceToTriggerSync = -1;
    private int mCurrentTargetOffset;
    private float mInitialMotionY;
    private float mLastMotionY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private View mTopView, mBottomView;
    private int mTopHeight, mBottomHeight;
    private STATUS mStatus = STATUS.NORMAL;
    private boolean mDisable; // 用来控制控件是否允许滚动
    private boolean mCurrentTopDragged = true;
    private boolean mRefrshEnabled = true;
    private boolean mLoadEnabled = true;
    private long mTimeLooseToRefresh = 500;
    private long mTimeRefreshToNormal =500;
    private long mTimeCancleRefresh = 500;
    private boolean mTouchEventInitial = true;

    //meterial disign
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private boolean mNestedScrollInProgress;
    private float mTotalUnconsumed;
    private boolean mUsingCustomStart;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];


    private enum STATUS {
        NORMAL, LOOSEN, REFRESHING
    }

    public RefreshAndLoadingLayout(Context context) {
        this(context, null);
    }

    public RefreshAndLoadingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshAndLoadingLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RefreshAndLoadingLayout, defStyleAttr, 0);
        mRefrshEnabled = ta.getBoolean(R.styleable.RefreshAndLoadingLayout_refreshEnabled, true);
        mLoadEnabled = ta.getBoolean(R.styleable.RefreshAndLoadingLayout_loadEnabled, true);
        ta.recycle();

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
        if (mTopView == null) {
            mTopView = getChildAt(0);
        }
        if (mBottomView == null) {
            mBottomView = getChildAt(2);
        }
        if (mTarget == null) {
            if (getChildCount() != 3 && !isInEditMode()) {
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
        }

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() != 3) {
            return;
        }
        final View child = getChildAt(1);
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        mTopView.layout(childLeft, childTop - mTopHeight, childLeft + childWidth, childTop);
        mBottomView.layout(childLeft, child.getMeasuredHeight(), childLeft + childWidth, child.getMeasuredHeight() + mBottomHeight);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getChildCount() != 3 && !isInEditMode()) {
            throw new IllegalStateException("RefreshAndLoadLayout can only host three children");
        }
        ensureTarget();
        measureChild(mTopView, widthMeasureSpec, heightMeasureSpec);
        mTopHeight = mTopView.getMeasuredHeight();
        mTopDistanceToTriggerSync = mTopHeight;
        measureChild(mBottomView, widthMeasureSpec, heightMeasureSpec);
        mBottomHeight = mBottomView.getMeasuredHeight();
        mBottomDistanceToTriggerSync = -mBottomHeight;
        mTarget.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
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
        if (mRefreshing) {
            return true;
        }
        ensureTarget();
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }
        if (mRefreshing) {
            return false;
        }
        if (!isEnabled() || mReturningToStart || canChildScrollUp() || canChildScrollDown() || mStatus == STATUS.REFRESHING || mNestedScrollInProgress) {
            if (canChildScrollUp() && canChildScrollDown())
                return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                mTouchEventInitial = true;
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
        if (mRefreshing) {
            return true;
        }
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
                mTouchEventInitial = true;
                break;

            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                final float y = ev.getY();
                final float yDiff = (y - mInitialMotionY) / 2;

                if (mTouchEventInitial) {
                    mCurrentTopDragged = yDiff > 0;
                    mTouchEventInitial = false;
                }

                if (!mRefrshEnabled && mCurrentTopDragged) {
                    return false;
                }
                if (!mLoadEnabled && !mCurrentTopDragged) {
                    return false;
                }
                if (!mIsBeingDragged && Math.abs(yDiff) > mTouchSlop) {
                    mIsBeingDragged = true;
                }

                if (mIsBeingDragged) {
                    updateContentOffsetTop((yDiff), mCurrentTopDragged);
                    if (yDiff > mTopDistanceToTriggerSync | yDiff < mBottomDistanceToTriggerSync) {
                        if (mStatus == STATUS.NORMAL) {
                            mStatus = STATUS.LOOSEN;
                            if (mListener != null) {
                                mListener.onLoose(mCurrentTopDragged);
                            }
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
                    cancleRefresh();
                }
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                return false;
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                return false;
        }

        return true;
    }

    private void cancleRefresh() {
        mStatus = STATUS.NORMAL;
        ValueAnimator anim;
        if (mCurrentTopDragged) {
            anim = ValueAnimator.ofInt(mCurrentTargetOffset, 0);
        } else {
            anim = ValueAnimator.ofInt(mCurrentTargetOffset, 0);
        }
        anim.setDuration(mTimeCancleRefresh);
        anim.setInterpolator(mDecelerateInterpolator);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int obj = (int) valueAnimator.getAnimatedValue();
                updateContentOffsetTop(obj, mCurrentTopDragged);
            }
        });
        if (mListener != null) {
            mListener.onNormal(mCurrentTopDragged);
        }
        anim.start();
    }

    private void startRefresh() {
        mRefreshing = true;
        mStatus = STATUS.REFRESHING;
        ValueAnimator anim;
        if (mCurrentTopDragged) {
            anim = ValueAnimator.ofInt(mCurrentTargetOffset, mTopDistanceToTriggerSync);
        } else {
            anim = ValueAnimator.ofInt(mCurrentTargetOffset, mBottomDistanceToTriggerSync);
        }
        anim.setDuration(mTimeLooseToRefresh);
        anim.setInterpolator(mDecelerateInterpolator);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int obj = (int) valueAnimator.getAnimatedValue();
                updateContentOffsetTop(obj, mCurrentTopDragged);
            }
        });
        if (mListener != null) {
            mListener.onRefresh(mCurrentTopDragged);
        }
        anim.start();
    }

    public void stopRefresh() {
        ValueAnimator anim;
        if (mCurrentTopDragged) {
            anim = ValueAnimator.ofInt(mTopDistanceToTriggerSync, 0);
        } else {
            anim = ValueAnimator.ofInt(mBottomDistanceToTriggerSync, 0);
        }
        anim.setDuration(mTimeRefreshToNormal);
        anim.setInterpolator(mDecelerateInterpolator);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mStatus = STATUS.NORMAL;
                int obj = (int) valueAnimator.getAnimatedValue();
                updateContentOffsetTop(obj, mCurrentTopDragged);
            }
        });
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mIsBeingDragged = false;
                mRefreshing = false;
                if (mListener != null) {
                    mListener.onNormal(mCurrentTopDragged);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        anim.start();
    }

    private void updateContentOffsetTop(float targetTop, boolean isTop) {
        setTargetOffsetTopAndBottom((int) (targetTop - mTarget.getTop()), isTop);
    }

    private void setTargetOffsetTopAndBottom(int offset, boolean isTop) {
        if (isTop) {
            mTopView.offsetTopAndBottom(offset);
            mTarget.offsetTopAndBottom(offset);
            mCurrentTargetOffset = mTarget.getTop();
        } else {
            mBottomView.offsetTopAndBottom(offset);
            mTarget.offsetTopAndBottom(offset);
            mCurrentTargetOffset = mTarget.getTop();
        }
        invalidate();
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

    private void nestedScroll(float mTotalUnconsumed) {

    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isEnabled() && !mReturningToStart && !mRefreshing
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            //  moveSpinner(mTotalUnconsumed);
        }

        if (mUsingCustomStart && dy > 0 && mTotalUnconsumed == 0
                && Math.abs(dy - consumed[1]) > 0) {

            mTopView.setVisibility(GONE);
            mBottomView.setVisibility(GONE);

        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        if (mTotalUnconsumed > 0) {
            mTopView.setVisibility(VISIBLE);
            mBottomView.setVisibility(VISIBLE);
            mTotalUnconsumed = 0;
        }
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed, final int dxUnconsumed, final int dyUnconsumed) {
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow);
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy);

            nestedScroll(mTotalUnconsumed);
        }
    }


    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }


}

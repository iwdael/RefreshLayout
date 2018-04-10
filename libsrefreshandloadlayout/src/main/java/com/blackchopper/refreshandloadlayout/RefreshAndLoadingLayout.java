

package com.blackchopper.refreshandloadlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;

/**
 * author  : Black Chopper
 * e-mail  : 4884280@qq.com
 * github  : http://github.com/BlackChopper
 * project : RefreshAndLoadingLayout
 */
public class RefreshAndLoadingLayout extends ViewGroup {
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
    private int mTimeLooseToRefresh = 500;
    private int mTimeRefreshToNormal = 500;
    private int mTimeCancleRefresh = 500;
    private boolean mTouchEventInitial = true;
    //当前是否在动画,拦截停止刷新过快，造成动画重叠
    private boolean mCurrentAnim = false;
    private boolean mContinueAnim = false;

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
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RefreshAndLoadingLayout, defStyleAttr, 0);
        mRefrshEnabled = ta.getBoolean(R.styleable.RefreshAndLoadingLayout_refreshEnabled, true);
        mLoadEnabled = ta.getBoolean(R.styleable.RefreshAndLoadingLayout_loadEnabled, true);
        mTimeRefreshToNormal = ta.getInt(R.styleable.RefreshAndLoadingLayout_refreshTime, 500);
        mTimeLooseToRefresh = ta.getInt(R.styleable.RefreshAndLoadingLayout_returnTime, 500);
        mTimeCancleRefresh = ta.getInt(R.styleable.RefreshAndLoadingLayout_cancelTime, 500);

        ta.recycle();

    }


    public void setOnRefreshListener(OnRefreshAndLoadListener listener) {
        mListener = listener;
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
                throw new IllegalStateException("RefreshAndLoadingLayout can only host three children");
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
            throw new IllegalStateException("RefreshAndLoadingLayout can only host three children");
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
        if (!isEnabled() || mReturningToStart) {
            return false;
        }
        if (canChildScrollUp() && canChildScrollDown()) {
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
                    if ((yDiff > mTopDistanceToTriggerSync & mCurrentTopDragged) | (yDiff < mBottomDistanceToTriggerSync & (!mCurrentTopDragged))) {
                        if (mStatus == STATUS.NORMAL) {
                            mStatus = STATUS.LOOSEN;
                            //  Notes.getInstence().register(this).logger("------------mStatus == STATUS.NORMAL----------------->>" + mCurrentTopDragged);
                            if (mListener != null) {
                                mListener.onLoose(mCurrentTopDragged);
                            }
                        }
                    } else {
                        if (mStatus == STATUS.LOOSEN) {
                            mStatus = STATUS.NORMAL;
                            if (mListener != null) {
                                 mListener.onNormal(mCurrentTopDragged);
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
        mCurrentAnim = true;
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
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                //     Notes.getInstence().register(this).logger("cancleRefresh----onAnimationStart--->>");

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                //      Notes.getInstence().register(this).logger("cancleRefresh----onAnimationEnd--->>");
                mCurrentAnim = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mCurrentAnim = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        anim.start();
    }

    private void startRefresh() {

        //  Notes.getInstence().register(this).logger("--------startRefresh-------------->>" + mCurrentTopDragged);

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
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                //    Notes.getInstence().register(this).logger("startRefresh----onAnimationStart--->>");
                mCurrentAnim = true;
                mRefreshing = true;
                mContinueAnim = false;
                mStatus = STATUS.REFRESHING;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                //      Notes.getInstence().register(this).logger("startRefresh----onAnimationEnd--->>");
                mCurrentAnim = false;
                if (mContinueAnim) {
                    mContinueAnim = false;
                    stopRefresh();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mCurrentAnim = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        anim.start();
        if (mListener != null) {
            mListener.onRefresh(mCurrentTopDragged);
        }
    }

    public void stopRefresh() {

        //     Notes.getInstence().register(this).logger("------stopRefresh------");
        if (mCurrentAnim) {
            mContinueAnim = true;
            //   Notes.getInstence().register(this).logger("------stopRefresh-----mContinueAnim->>" + true);
            return;
        }
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
                //  Notes.getInstence().register(this).logger("stopRefresh----onAnimationStart--->>");
                mCurrentAnim = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                //    Notes.getInstence().register(this).logger("stopRefresh----onAnimationEnd--->>");
                mCurrentAnim = false;
                mIsBeingDragged = false;
                mRefreshing = false;
                if (mListener != null) {
                    mListener.onNormal(mCurrentTopDragged);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mCurrentAnim = false;
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


}

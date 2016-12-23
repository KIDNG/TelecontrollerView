package cn.kidng.telecontrollerview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by kidng on 2016/12/20.
 */

public class TelecontrollerView extends View {
  private static final int DEFAULT_CENTER_CIRCLE_RADIUS_DP = 5;
  private static final int DEFAULT_OUT_MENU_WIDTH_DP = 5;
  private static final int DEFAULT_CENTER_MARGIN_DP = 5;
  private static final int DEFAULT_MENU_MARGIN_DP = 5;
  private static final int FLAG_CENTER = 0;
  private static final int FLAG_UP = 1;
  private static final int FLAG_RIGHT = 2;
  private static final int FLAG_DOWN = 3;
  private static final int FLAG_LEFT = 4;

  private int mCenterCircleRadius;
  private int mCenterMargin;
  private int mMenuWidth;
  private int mMenuAngle;

  private Path mUpPath = new Path();
  private Path mDownPath = new Path();
  private Path mLeftPath = new Path();
  private Path mRightPath = new Path();
  private Path mCenterPath = new Path();

  private Region mUpRegion = new Region();
  private Region mDownRegion = new Region();
  private Region mLeftRegion = new Region();
  private Region mRightRegion = new Region();
  private Region mCenterRegion = new Region();

  private int mTouchFlag = -1;
  private int mClickFlag = -1;

  private OnMenuClickListener mMenuClickListener;

  private int mDefaultColor = 0xFFDF9C81;
  private int mTouchedColor = 0xFF4E5268;

  private Paint mPaint = new Paint();
  private int mWidth;
  private int mMidWidth;
  private int mHeight;
  private int mMidHeight;

  public TelecontrollerView(Context context) {
    this(context, null);
  }

  public TelecontrollerView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TelecontrollerView(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public TelecontrollerView(Context context, AttributeSet attrs, int defStyleAttr,
      int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context, attrs, defStyleAttr, defStyleRes);
  }

  private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    TypedArray ta =
        context.obtainStyledAttributes(attrs, R.styleable.TelecontrollerView, defStyleAttr,
            defStyleRes);
    mCenterCircleRadius =
        ta.getDimensionPixelSize(R.styleable.TelecontrollerView_centerCircleRadius,
            dp2px(DEFAULT_CENTER_CIRCLE_RADIUS_DP));
    mCenterMargin = ta.getDimensionPixelSize(R.styleable.TelecontrollerView_centerMargin,
        dp2px(DEFAULT_CENTER_MARGIN_DP));
    mMenuWidth = ta.getDimensionPixelSize(R.styleable.TelecontrollerView_outMenuWidth,
        dp2px(DEFAULT_OUT_MENU_WIDTH_DP));
    mMenuAngle = ta.getInt(R.styleable.TelecontrollerView_menuAngle, 80);
    ta.recycle();
    if (mMenuAngle < 0 || mMenuAngle > 90) {
      throw new IllegalArgumentException("the menu angle must in 0~90");
    }
    //setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    mPaint.setAntiAlias(true);
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mWidth = w;
    mMidWidth = w / 2;
    mHeight = h;
    mMidHeight = h / 2;

    //将剪裁边界设置为视图大小
    Region globalRegion = new Region(-w, -h, w, h);

    //计算4个menu内圆
    RectF innerCircleRectF =
        new RectF(-mCenterCircleRadius - mCenterMargin, -mCenterCircleRadius - mCenterMargin,
            mCenterCircleRadius + mCenterMargin, mCenterCircleRadius + mCenterMargin);
    //计算4个menu外圆
    RectF outCircleRectF =
        new RectF(innerCircleRectF.left - mMenuWidth, innerCircleRectF.top - mMenuWidth,
            innerCircleRectF.right + mMenuWidth, innerCircleRectF.bottom + mMenuWidth);

    //坐标原点在每次绘制菜单时会移至view中心
    mCenterPath.reset();
    mCenterPath.addCircle(0, 0, mCenterCircleRadius, Path.Direction.CW);
    mCenterRegion.setPath(mCenterPath, globalRegion);

    mLeftPath.reset();
    mLeftPath.addArc(outCircleRectF, 180 - mMenuAngle / 2, mMenuAngle);
    mLeftPath.arcTo(innerCircleRectF, 180 + mMenuAngle / 2 - 2, -mMenuAngle + 4);
    mLeftPath.close();
    mLeftRegion.setPath(mLeftPath, globalRegion);

    mUpPath.reset();
    mUpPath.addArc(outCircleRectF, 270 - mMenuAngle / 2, mMenuAngle);
    mUpPath.arcTo(innerCircleRectF, 270 + mMenuAngle / 2 - 2, -mMenuAngle + 4);
    mUpPath.close();
    mUpRegion.setPath(mUpPath, globalRegion);

    mRightPath.reset();
    mRightPath.addArc(outCircleRectF, -mMenuAngle / 2, mMenuAngle);
    mRightPath.arcTo(innerCircleRectF, mMenuAngle / 2 - 2, -mMenuAngle + 4);
    mRightPath.close();
    mRightRegion.setPath(mRightPath, globalRegion);

    mDownPath.reset();
    mDownPath.addArc(outCircleRectF, 90 - mMenuAngle / 2, mMenuAngle);
    mDownPath.arcTo(innerCircleRectF, 90 + mMenuAngle / 2 - 2, -mMenuAngle + 4);
    mDownPath.close();
    mDownRegion.setPath(mDownPath, globalRegion);
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    onDrawCenterMenu(canvas);
    onDrawLeftMenu(canvas);
    onDrawRightMenu(canvas);
    onDrawUpMenu(canvas);
    onDrawDownMenu(canvas);
  }

  private void onDrawLeftMenu(Canvas canvas) {
    canvas.save();
    canvas.translate(mMidWidth, mMidHeight);
    if (mClickFlag == FLAG_LEFT) {
      mPaint.setColor(mDefaultColor);
    } else {
      mPaint.setColor(mTouchedColor);
    }
    mPaint.setStyle(Paint.Style.FILL);
    canvas.drawPath(mLeftPath, mPaint);
    canvas.restore();
  }

  private void onDrawRightMenu(Canvas canvas) {
    canvas.save();
    canvas.translate(mMidWidth, mMidHeight);
    if (mClickFlag == FLAG_RIGHT) {
      mPaint.setColor(mDefaultColor);
    } else {
      mPaint.setColor(mTouchedColor);
    }
    mPaint.setStyle(Paint.Style.FILL);
    canvas.drawPath(mRightPath, mPaint);
    canvas.restore();
  }

  private void onDrawUpMenu(Canvas canvas) {
    canvas.save();
    canvas.translate(mMidWidth, mMidHeight);
    if (mClickFlag == FLAG_UP) {
      mPaint.setColor(mDefaultColor);
    } else {
      mPaint.setColor(mTouchedColor);
    }
    mPaint.setStyle(Paint.Style.FILL);
    canvas.drawPath(mUpPath, mPaint);
    canvas.restore();
  }

  private void onDrawDownMenu(Canvas canvas) {
    canvas.save();
    canvas.translate(mMidWidth, mMidHeight);
    if (mClickFlag == FLAG_DOWN) {
      mPaint.setColor(mDefaultColor);
    } else {
      mPaint.setColor(mTouchedColor);
    }
    mPaint.setStyle(Paint.Style.FILL);
    canvas.drawPath(mDownPath, mPaint);
    canvas.restore();
  }

  private void onDrawCenterMenu(Canvas canvas) {
    canvas.save();
    canvas.translate(mMidWidth, mMidHeight);
    if (mClickFlag == FLAG_CENTER) {
      mPaint.setColor(mDefaultColor);
    } else {
      mPaint.setColor(mTouchedColor);
    }
    mPaint.setStyle(Paint.Style.FILL);
    canvas.drawPath(mCenterPath, mPaint);
    canvas.restore();
  }

  @Override public boolean onTouchEvent(MotionEvent event) {
    //将点击坐标系移至绘制坐标系
    int x = (int) (event.getX() - mMidWidth);
    int y = (int) (event.getY() - mMidHeight);
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        mTouchFlag = getTouchFlagType(x, y);
        mClickFlag = mTouchFlag;
        break;
      case MotionEvent.ACTION_MOVE:
        mTouchFlag = getTouchFlagType(x, y);
        break;
      case MotionEvent.ACTION_UP:
        mTouchFlag = getTouchFlagType(x, y);
        // 如果手指按下区域和抬起区域相同且不为空，则判断点击事件
        if (mClickFlag == mTouchFlag && mClickFlag != -1 && mMenuClickListener != null) {
          if (mClickFlag == FLAG_CENTER) {
            mMenuClickListener.onCenterMenuClick();
          } else if (mClickFlag == FLAG_UP) {
            mMenuClickListener.onUpMenuClick();
          } else if (mClickFlag == FLAG_RIGHT) {
            mMenuClickListener.onRightMenuClick();
          } else if (mClickFlag == FLAG_DOWN) {
            mMenuClickListener.onDownMenuClick();
          } else if (mClickFlag == FLAG_LEFT) {
            mMenuClickListener.onLeftMenuClick();
          }
        }
        mTouchFlag = mClickFlag = -1;
        break;
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_OUTSIDE:
        mTouchFlag = mClickFlag = -1;
        break;
    }
    invalidate();
    return true;
  }

  private int getTouchFlagType(int x, int y) {
    if (mCenterRegion.contains(x, y)) {
      return FLAG_CENTER;
    } else if (mUpRegion.contains(x, y)) {
      return FLAG_UP;
    } else if (mRightRegion.contains(x, y)) {
      return FLAG_RIGHT;
    } else if (mDownRegion.contains(x, y)) {
      return FLAG_DOWN;
    } else if (mLeftRegion.contains(x, y)) {
      return FLAG_LEFT;
    }
    return -1;
  }

  public int dp2px(float dipValue) {
    final float scale = getContext().getResources().getDisplayMetrics().density;
    return (int) (dipValue * scale + 0.5f);
  }

  public void setOnMenuClickListener(OnMenuClickListener listener) {
    mMenuClickListener = listener;
  }

  public interface OnMenuClickListener {
    void onCenterMenuClick();

    void onUpMenuClick();

    void onRightMenuClick();

    void onDownMenuClick();

    void onLeftMenuClick();
  }
}

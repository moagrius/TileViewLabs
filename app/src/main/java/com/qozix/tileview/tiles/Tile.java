package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.animation.AnimationUtils;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.geom.FloatMathHelper;
import com.qozix.tileview.graphics.BitmapProvider;

public class Tile {

  public enum State {
    UNASSIGNED,
    PENDING_DECODE,
    DECODED
  }

  private static final int DEFAULT_TRANSITION_DURATION = 200;

  private State mState = State.UNASSIGNED;

  private int mWidth;
  private int mHeight;
  private int mLeft;
  private int mTop;
  private int mRight;
  private int mBottom;

  private float mProgress = 1f;
  private float mOpacity;

  private int mRow;
  private int mColumn;

  private float mDetailLevelScale;

  private boolean mHasReportedDirtyAtFullOpacity;

  private Object mData;
  private Bitmap mBitmap;

  private Rect mIntrinsicRect = new Rect();
  private Rect mBaseRect = new Rect();
  private Rect mRelativeRect = new Rect();
  private Rect mScaledRect = new Rect();

  public double mRenderTimeStamp;

  private boolean mTransitionsEnabled;

  private int mTransitionDuration = DEFAULT_TRANSITION_DURATION;

  private Paint mPaint;

  private DetailLevel mDetailLevel;

  public Tile( int column, int row, int width, int height, Object data, DetailLevel detailLevel ) {
    mRow = row;
    mColumn = column;
    mWidth = width;
    mHeight = height;
    mLeft = column * width;
    mTop = row * height;
    mRight = mLeft + mWidth;
    mBottom = mTop + mHeight;
    mData = data;
    mDetailLevel = detailLevel;
    mDetailLevelScale = mDetailLevel.getScale();
    mIntrinsicRect.set( 0, 0, mWidth, mHeight );
    mBaseRect.set( mLeft, mTop, mRight, mBottom );  // TODO: need this?
    mRelativeRect.set(
      FloatMathHelper.unscale( mLeft, mDetailLevelScale ),
      FloatMathHelper.unscale( mTop, mDetailLevelScale ),
      FloatMathHelper.unscale( mRight, mDetailLevelScale ),
      FloatMathHelper.unscale( mBottom, mDetailLevelScale )
    );
    mScaledRect.set( mRelativeRect );
  }

  public int getWidth() {
    return mWidth;
  }

  public int getHeight() {
    return mHeight;
  }

  public int getLeft() {
    return mLeft;
  }

  public int getTop() {
    return mTop;
  }

  public int getRow() {
    return mRow;
  }

  public int getColumn() {
    return mColumn;
  }

  public Object getData() {
    return mData;
  }

  public Bitmap getBitmap() {
    return mBitmap;
  }

  public boolean hasBitmap() {
    return mBitmap != null;
  }

  public Rect getBaseRect() {
    return mBaseRect;
  }

  public Rect getScaledRect( float scale ) {
    mScaledRect.set(
      (int) (mRelativeRect.left * scale),
      (int) (mRelativeRect.top * scale),
      (int) (mRelativeRect.right * scale),
      (int) (mRelativeRect.bottom * scale)
    );
    return mScaledRect;
  }

  public void setTransitionDuration( int transitionDuration ) {
    mTransitionDuration = transitionDuration;
  }

  public State getState() {
    return mState;
  }

  public void setState( State state ) {
    mState = state;
  }

  public void computeProgress(){
    if( !mTransitionsEnabled ) {
      return;
    }
    double now = AnimationUtils.currentAnimationTimeMillis();
    double elapsed = now - mRenderTimeStamp;
    mProgress = (float) Math.min( 1, elapsed / mTransitionDuration );
    if( mProgress == 1 ) {
      mTransitionsEnabled = false;
    }
  }

  public boolean composeWithOpacity(){
    computeProgress();
    return mProgress < 1f;
  }

  public void stampTime() {
    mRenderTimeStamp = AnimationUtils.currentAnimationTimeMillis();
  }


  public void setTransitionsEnabled( boolean enabled ) {
    mTransitionsEnabled = enabled;
  }

  public DetailLevel getDetailLevel() {
    return mDetailLevel;
  }

  /**
   * @deprecated
   * @return
   */
  public float getRendered() {
    return mProgress;
  }

  public boolean getIsDirty() {
    return mTransitionsEnabled && mProgress < 1f;
  }

  public boolean getIsDirty2() {
    if( !mTransitionsEnabled ) {
      return false;
    }
    if( mProgress < 1f ) {
      mHasReportedDirtyAtFullOpacity = false;
      return true;
    }
    if( mHasReportedDirtyAtFullOpacity ) {
      return false;
    }
    mHasReportedDirtyAtFullOpacity = true;
    return true;
  }

  public Paint getPaint() {
    if( !mTransitionsEnabled ) {
      return mPaint = null;
    }
    if( mPaint == null ) {
      mPaint = new Paint();
    }
    mPaint.setAlpha( (int) (255 * mOpacity) );
    return mPaint;
  }

  void generateBitmap( Context context, BitmapProvider bitmapProvider ) {
    if( mBitmap != null ) {
      return;
    }
    mBitmap = bitmapProvider.getBitmap( this, context );
    mState = State.DECODED;
  }

  void destroy( boolean shouldRecycle ) {
    mState = State.UNASSIGNED;
    if( shouldRecycle && mBitmap != null && !mBitmap.isRecycled() ) {
      mBitmap.recycle();
    }
    mBitmap = null;
  }

  /**
   * @param canvas The canvas the tile's bitmap should be drawn into
   * @return True if the tile is dirty (drawing output has changed and needs parent validation)
   */
  boolean draw( Canvas canvas ) {  // TODO: this might squish edge images
    if( mBitmap != null ) {
      canvas.drawBitmap( mBitmap, mIntrinsicRect, mRelativeRect, getPaint() );
    }
    return getIsDirty();
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + getColumn();
    hash = hash * 31 + getRow();
    hash = hash * 31 + (int) (1000 * getDetailLevel().getScale());
    return hash;
  }

  @Override
  public boolean equals( Object o ) {
    if( this == o ) {
      return true;
    }
    if( o instanceof Tile ) {
      Tile m = (Tile) o;
      return m.getRow() == getRow()
        && m.getColumn() == getColumn()
        && m.getDetailLevel().getScale() == getDetailLevel().getScale();
    }
    return false;
  }

  public String toShortString(){
    return mColumn + ":" + mRow;
  }

}

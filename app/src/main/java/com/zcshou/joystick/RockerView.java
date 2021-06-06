package com.zcshou.joystick;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.zcshou.gogogo.R;

public class RockerView extends View {
    private Paint outerCirclePaint;
    private Paint innerCirclePaint;
    /** 内圆中心x坐标 */
    private double innerCenterX;
    /** 内圆中心y坐标 */
    private double innerCenterY;
    /** view中心点x坐标 */
    private float viewCenterX;
    /** view中心点y左边 */
    private float viewCenterY;
    /** 外圆半径 */
    private int outerCircleRadius;
    /** 内圆半径 */
    private int innerCircleRadius;

    private Bitmap mRockerBitmap = null;
    private boolean isAuto = false;
    private boolean isClick = false;

    private RockerViewClickListener mListener;
    Context mContext;

    Rect srcRect = null;
    Rect dstRect = null;

    public RockerView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public RockerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        outerCirclePaint = new Paint();
        outerCirclePaint.setColor(ContextCompat.getColor(mContext, R.color.lightgrey));
        outerCirclePaint.setAlpha(180);
        outerCirclePaint.setAntiAlias(true);

        innerCirclePaint = new Paint();
        innerCirclePaint.setColor(ContextCompat.getColor(mContext, R.color.grey));
        innerCirclePaint.setAlpha(180);
        innerCirclePaint.setAntiAlias(true);

        isAuto = true;
        Bitmap bitmap = getBitmap(getContext(), R.drawable.ic_lock_close);
        Matrix mMatrix = new Matrix();
        mMatrix.postScale(0.4f,0.4f);
        mRockerBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mMatrix,true);

        srcRect = new Rect(0, 0, mRockerBitmap.getWidth(), mRockerBitmap.getHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int size = getMeasuredWidth();
        setMeasuredDimension(size, size);

        innerCenterX = (double)size / 2;
        innerCenterY = (double)size / 2;
        viewCenterX = (float)size / 2;
        viewCenterY = (float)size / 2;
        outerCircleRadius = size / 2;
        innerCircleRadius = size / 6;

        if (dstRect == null) {
            dstRect = new Rect(
                    (int) (innerCenterX - mRockerBitmap.getWidth()),
                    (int) (innerCenterY - mRockerBitmap.getHeight()),
                    (int) (innerCenterX + mRockerBitmap.getWidth()),
                    (int) (innerCenterY + mRockerBitmap.getHeight()));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(viewCenterX, viewCenterY, outerCircleRadius, outerCirclePaint);
        /* 摇杆的控制部分由两部分组成 */
        canvas.drawCircle((float) innerCenterX, (float) innerCenterY, innerCircleRadius, innerCirclePaint);
        canvas.drawBitmap(mRockerBitmap, srcRect, dstRect, innerCirclePaint);
    }

    @Override
    public boolean performClick() {
        // Calls the super implementation, which generates an AccessibilityEvent
        // and calls the onClick() listener on the view, if any
        super.performClick();

        // Handle the action for the custom click here

        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                /* 如果初始点击位置 不再内圆中,返回 false 将不再继续处理后续事件 */
                if (event.getX() < innerCenterX - innerCircleRadius || event.getX() > innerCenterX + innerCircleRadius
                || event.getY() < innerCenterY - innerCircleRadius || event.getY() > innerCenterY + innerCircleRadius)
                {
                    return true;
                }
                isClick = true;
                break;
            case MotionEvent.ACTION_MOVE:
                moveToPosition(event.getX(), event.getY());
                isClick = false;
                break;
            case MotionEvent.ACTION_UP:
                if (isClick) {
                    isClick = false;
                    changeRockerCtrl();
                    invalidate();
                }
                if (!isAuto) {
                    moveToPosition(viewCenterX, viewCenterY);
                }
                performClick();
                break;
        }

        return true;
    }

    private void moveToPosition(float x, float y) {
        double distance = Math.sqrt(Math.pow(x-viewCenterX, 2) + Math.pow(y-viewCenterY, 2)); //触摸点与view中心距离

        if (distance < outerCircleRadius-innerCircleRadius) {
            //在自由域之内，触摸点实时作为内圆圆心
            innerCenterX = x;
            innerCenterY = y;
        } else {
            //在自由域之外，内圆圆心在触摸点与外圆圆心的线段上
            int innerDistance = outerCircleRadius-innerCircleRadius;  //内圆圆心到中心点距离
            //相似三角形的性质，两个相似三角形各边比例相等得到等式
            innerCenterX = (x-viewCenterX)*innerDistance/distance + viewCenterX;
            innerCenterY = (y-viewCenterY)*innerDistance/distance + viewCenterY;
        }

        dstRect = new Rect(
                (int) (innerCenterX - mRockerBitmap.getWidth()),
                (int) (innerCenterY - mRockerBitmap.getHeight()),
                (int) (innerCenterX + mRockerBitmap.getWidth()),
                (int) (innerCenterY + mRockerBitmap.getHeight()));

        invalidate();
        double angle = Math.toDegrees(Math.atan2((innerCenterX - viewCenterX), (innerCenterY-viewCenterY))) - 90;
        double r = Math.sqrt(Math.pow(innerCenterX - viewCenterX, 2) + Math.pow(innerCenterY-viewCenterY, 2)) / (outerCircleRadius-innerCircleRadius);
        mListener.clickAngleInfo(true, angle, r);
    }

    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private static Bitmap getBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return BitmapFactory.decodeResource(context.getResources(), drawableId);
        } else if (drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        } else {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }

    private void changeRockerCtrl() {
        Bitmap bitmap;
        if (mRockerBitmap != null) {
            mRockerBitmap.recycle();
        }
        if (isAuto) {
            bitmap = getBitmap(getContext(), R.drawable.ic_lock_open);
        } else {
            bitmap = getBitmap(getContext(), R.drawable.ic_lock_close);
        }
        isAuto = !isAuto;

        Matrix mMatrix = new Matrix();
        mMatrix.postScale(0.4f,0.4f);
        mRockerBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mMatrix,true);

        dstRect = new Rect(
                (int) (innerCenterX - mRockerBitmap.getWidth()),
                (int) (innerCenterY - mRockerBitmap.getHeight()),
                (int) (innerCenterX + mRockerBitmap.getWidth()),
                (int) (innerCenterY + mRockerBitmap.getHeight()));
    }

    public void setListener(RockerViewClickListener mListener) {
        this.mListener = mListener;
    }

    public interface RockerViewClickListener {
        /**
         * 点击的角度信息
         */
        void clickAngleInfo(boolean auto, double angle, double r);
    }
}
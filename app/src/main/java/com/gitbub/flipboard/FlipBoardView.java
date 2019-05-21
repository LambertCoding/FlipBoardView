package com.gitbub.flipboard;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 仿写红板报动画的View
 *
 * @author yu
 */
public class FlipBoardView extends View {
    // 翻起的一半,Y轴方向旋转角度
    private float degreeY;
    // 不变的那一半，Y轴方向旋转角度
    private float fixDegreeY;
    // Z轴方向（平面内）旋转的角度
    private float degreeZ;

    // 最终执行完动画后，图片翻起的角度
    private int finalAngle;

//    private Paint paint;
//    private Bitmap bitmap;
    private BitmapDrawable drawable;
    private Camera camera;
    private AnimatorSet animatorSet;

    public FlipBoardView(Context context) {
        this(context, null);
    }

    public FlipBoardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlipBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlipBoardView);
        drawable = (BitmapDrawable) a.getDrawable(R.styleable.FlipBoardView_fbv_bitmap);
        finalAngle = a.getInteger(R.styleable.FlipBoardView_fbv_angleY, 30);
        a.recycle();

        if (drawable == null) {
            drawable = (BitmapDrawable) getResources().getDrawable(R.mipmap.flip_board);
        }

//        bitmap = drawable.getBitmap();
//
//        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        camera = new Camera();
        camera.setLocation(0, 0, getResources().getDisplayMetrics().density * 8);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int bitmapWidth = drawable.getIntrinsicWidth();
        int bitmapHeight = drawable.getIntrinsicHeight();

        int paddingStart = getPaddingStart();
        int paddingTop = getPaddingTop();
        int paddingEnd = getPaddingEnd();
        int paddingBottom = getPaddingBottom();

        setMeasuredDimension(resolveSize(bitmapWidth + paddingStart + paddingEnd, widthMeasureSpec),
                resolveSize(bitmapHeight + paddingTop + paddingBottom, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int bitmapCenterX = drawable.getIntrinsicWidth() >> 1;
        int bitmapCenterY = drawable.getIntrinsicHeight() >> 1;
        int centerX = getWidth() >> 1;
        int centerY = getHeight() >> 1;

        // 分为两步：1，画3D翻起的一半  2，画正常状态的一半，具体又分几步：
        // 先移动画布到中心，让camera旋转效果居中
        // 旋转画布
        // 裁切需要绘制的范围
        // 运用camera旋转效果
        canvas.save();
        camera.save();

        canvas.translate(centerX, centerY);
        canvas.rotate(-degreeZ);

        camera.rotateY(degreeY);
        camera.applyToCanvas(canvas);
        camera.restore();

        // 注意坐标系已经移动到中心了
        canvas.clipRect(0, -centerY, centerX, centerY);

        canvas.rotate(degreeZ);
        canvas.translate(-centerX, -centerY);

        drawable.setBounds(centerX - bitmapCenterY, centerY - bitmapCenterY,
                centerX + bitmapCenterX, centerY + bitmapCenterY);
        drawable.draw(canvas);
//        canvas.drawBitmap(bitmap, centerX - bitmapCenterX, centerY - bitmapCenterY, paint);
        canvas.restore();


        // 画另一半
        canvas.save();
        camera.save();

        canvas.translate(centerX, centerY);
        canvas.rotate(-degreeZ);

        camera.rotateY(fixDegreeY);
        camera.applyToCanvas(canvas);
        camera.restore();

        canvas.clipRect(-centerX, -centerY, 0, centerY);

        canvas.rotate(degreeZ);
        canvas.translate(-centerX, -centerY);

        drawable.setBounds(centerX - bitmapCenterY, centerY - bitmapCenterY,
                centerX + bitmapCenterX, centerY + bitmapCenterY);
        drawable.draw(canvas);
//        canvas.drawBitmap(bitmap, centerX - bitmapCenterY, centerY - bitmapCenterY, paint);
        canvas.restore();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            start();
        } else {
            stop();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    public void start() {
        if (animatorSet == null) {
            ValueAnimator startAnim = ValueAnimator.ofFloat(0, finalAngle);
            startAnim.setDuration(600);
            startAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setDegreeY((Float) animation.getAnimatedValue());
                }
            });
            startAnim.setStartDelay(200);

            ValueAnimator middleAnim = ValueAnimator.ofFloat(0, 270);
            middleAnim.setDuration(800);
            middleAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setDegreeZ((Float) animation.getAnimatedValue());
                }
            });
            middleAnim.setStartDelay(200);

            ValueAnimator endAnim = ValueAnimator.ofFloat(0, -finalAngle);
            endAnim.setDuration(600);
            endAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setFixDegreeY((Float) animation.getAnimatedValue());
                }
            });
            endAnim.setStartDelay(200);

            animatorSet = new AnimatorSet();
            animatorSet.playSequentially(startAnim, middleAnim, endAnim);

            animatorSet.start();
        } else if (!animatorSet.isStarted()) {
            animatorSet.start();
        }
    }

    public void stop() {
        if (animatorSet != null) {
            animatorSet.removeAllListeners();
            animatorSet.cancel();
            animatorSet = null;
        }
    }

    public void setDegreeY(float degreeY) {
        this.degreeY = degreeY;
        invalidate();
    }

    public void setFixDegreeY(float fixDegreeY) {
        this.fixDegreeY = fixDegreeY;
        invalidate();
    }

    public void setDegreeZ(float degreeZ) {
        this.degreeZ = degreeZ;
        invalidate();
    }
}

package com.example.hearthbeat;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomPanImageView extends AppCompatImageView {
    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();
    private final PointF startPoint = new PointF();
    private final PointF midPoint = new PointF();
    private float oldDistance = 1.0f;
    private int mode; // 0: None, 1: Drag, 2: Zoom

    public ZoomPanImageView(final Context context) {
        super(context);
        this.init();
    }

    public ZoomPanImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    private void init() {
        this.setScaleType(ScaleType.MATRIX);
    }
    /*
        @Override
        public boolean onTouchEvent(final MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    this.matrix.set(this.getImageMatrix());
                    this.savedMatrix.set(this.matrix);
                    this.startPoint.set(event.getX(), event.getY());
                    this.mode = 1; // Set mode to Drag
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    this.oldDistance = this.spacing(event);
                    if (10.0f < oldDistance) {
                        this.savedMatrix.set(this.matrix);
                        this.midPoint(this.midPoint, event);
                        this.mode = 2; // Set mode to Zoom
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    this.mode = 0; // Reset mode
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (1 == mode) {
                        this.matrix.set(this.savedMatrix);
                        this.matrix.postTranslate(event.getX() - this.startPoint.x, event.getY() - this.startPoint.y);
                    } else if (2 == mode) {
                        final float newDistance = this.spacing(event);
                        if (10.0f < newDistance) {
                            this.matrix.set(this.savedMatrix);
                            final float scale = newDistance / this.oldDistance;
                            this.matrix.postScale(scale, scale, this.midPoint.x, this.midPoint.y);
                        }
                    }
                    break;
            }
            this.setImageMatrix(this.matrix);
            return true;
        }
    */
    private float spacing(final MotionEvent event) {
        final float x = event.getX(0) - event.getX(1);
        final float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(final PointF point, final MotionEvent event) {
        final float x = event.getX(0) + event.getX(1);
        final float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}

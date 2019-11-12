package edu.bu.metcs.fuzzychart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;

public class DiagramView extends View {
    Paint paint;
    Path path;

    public DiagramView(Context context) {
        super(context);
        path = new Path();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(5);
    }

    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                path = new Path();
                Point p = new Point((int) event.getX(), (int) event.getY());
                path.moveTo(p.x, p.y);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                Point p = new Point((int) event.getX(), (int) event.getY());
                path.lineTo(p.x, p.y);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                path.close();
                break;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }
}
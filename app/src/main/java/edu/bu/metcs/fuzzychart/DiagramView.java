package edu.bu.metcs.fuzzychart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class DiagramView extends View {
    Controller controller = Controller.getController();
    ArrayList<Shape> diagramShapes;
    Path diagramPath;
    Shape handDrawnShape;
    Path handDrawnPath;
    Paint shapeAttributes;

    public DiagramView(Context context) {
        super(context);
        diagramShapes = new ArrayList<>();
        diagramPath = new Path();
        handDrawnPath = new Path();
        shapeAttributes = new Paint();
        shapeAttributes.setAntiAlias(true);
        shapeAttributes.setStyle(Paint.Style.STROKE);
        shapeAttributes.setColor(Color.BLUE);
        shapeAttributes.setStrokeWidth(5);
    }

    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                handDrawnShape = new Shape();
                handDrawnPath = new Path();
                Point vertex = new Point((int) event.getX(), (int) event.getY());
                handDrawnShape.vertices.add(vertex);
                handDrawnPath.moveTo(vertex.x, vertex.y);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                Point vertex = new Point((int) event.getX(), (int) event.getY());
                handDrawnShape.vertices.add(vertex);
                handDrawnPath.lineTo(vertex.x, vertex.y);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                // Shape diagramShape = controller.processHandDrawnShape(handDrawnShape);
                // diagramShapes.add(diagramShape);

                diagramShapes.add(handDrawnShape); // delete

                // diagramPath.addPath(convertShapeToPath(diagramShape));

                diagramPath.addPath(convertShapeToPath(handDrawnShape)); // delete

                handDrawnPath.reset();
                invalidate();
                break;
            }
        }
        return true;
    }

    private Path convertShapeToPath(Shape shape) {
        Path path = new Path();

        Point startPoint = shape.vertices.remove(0);
        path.moveTo(startPoint.x, startPoint.y);

        for (Point point : shape.vertices) {
            path.lineTo(point.x, point.y);
        }

        return path;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(diagramPath, shapeAttributes);
        canvas.drawPath(handDrawnPath, shapeAttributes);
    }
}
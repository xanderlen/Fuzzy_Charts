package edu.bu.metcs.fuzzychart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class DiagramView extends View {
    Controller controller = Controller.getController();
    String diagramMode = "Drawing";
    Paint shape_Attributes;

    ArrayList<Shape> diagramShapes;
    Path diagramShapes_Path;

    Shape selectedShape;
    Path selectedShape_Path;

    Shape handDrawnShape;
    Path handDrawnShape_Path;

    public DiagramView(Context context) {
        super(context);
        diagramShapes = new ArrayList<>();
        diagramShapes_Path = new Path();
        handDrawnShape_Path = new Path();
        shape_Attributes = new Paint();
        shape_Attributes.setAntiAlias(true);
        shape_Attributes.setStyle(Paint.Style.STROKE);
        shape_Attributes.setColor(Color.BLUE);
        shape_Attributes.setStrokeWidth(5);
    }

    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        switch (diagramMode) {
            case "Drawing":
                processDrawingEvent(event);
                break;
            case "Selection":
                processSelectionEvent(event);
                break;
        }
        return true;
    }

    private void processDrawingEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                handDrawnShape = new Shape();
                handDrawnShape_Path = new Path();
                Point vertex = new Point((int) event.getX(), (int) event.getY());
                handDrawnShape.getVertices().add(vertex);
                handDrawnShape_Path.moveTo(vertex.x, vertex.y);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                Point vertex = new Point((int) event.getX(), (int) event.getY());
                handDrawnShape.getVertices().add(vertex);
                handDrawnShape_Path.lineTo(vertex.x, vertex.y);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                Shape diagramShape = controller.processHandDrawnShape(handDrawnShape);
                if (diagramShape.getShapeType().equals("Dot")) {
                    diagramMode = "Selection";
                    selectShapeEnclosingPoint(diagramShape.getVertices().get(0));
                } else {
                    diagramShapes.add(diagramShape);
                    diagramShapes_Path.addPath(convertShapeToPath(diagramShape));
                    handDrawnShape_Path.reset();
                    invalidate();
                }
                break;
            }

            case MotionEvent.ACTION_BUTTON_PRESS: {
                Log.e("Touch Event", "Button Pressed.");
                break;
            }
        }
    }

    private void processSelectionEvent(MotionEvent event) {

    }

    private void selectShapeEnclosingPoint(Point point) {
        selectedShape = null;
        for (Shape shape : diagramShapes) {
            if (shape.getVertices().get(0).equals(shape.getVertices().get(shape.getSize() -1))) {
                if (shape.shapeEnclosesPoint(point)) {
                    selectedShape = shape;
                    break;
                }
            }
        }

        if (selectedShape != null) {
            selectedShape_Path = convertShapeToPath(selectedShape);
        }
    }

    private Path convertShapeToPath(Shape shape) {
        Path path = new Path();

        Point startPoint = shape.getVertices().get(0);
        path.moveTo(startPoint.x, startPoint.y);

        for (int pointNum = 1; pointNum < shape.getSize(); pointNum++) {
            Point point = shape.getVertices().get(pointNum);
            path.lineTo(point.x, point.y);
        }

        return path;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(diagramShapes_Path, shape_Attributes);
        canvas.drawPath(selectedShape_Path, selectedShape_Attributes);
        canvas.drawPath(handDrawnShape_Path, shape_Attributes);
    }
}
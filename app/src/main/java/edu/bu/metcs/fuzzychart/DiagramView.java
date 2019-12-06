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
    String diagramMode;
    boolean mouseDragged;
    Point currentMousePosition;
    Paint shape_Attributes;
    Paint selectedShape_Attributes;

    ArrayList<Shape> diagramShapes;
    Path diagramShapes_Path;

    Shape selectedShape;
    Path selectedShape_Path;

    Shape handDrawnShape;
    Path handDrawnShape_Path;

    public DiagramView(Context context) {
        super(context);
        diagramMode = "Drawing";

        diagramShapes = new ArrayList<>();
        diagramShapes_Path = new Path();

        shape_Attributes = new Paint();
        shape_Attributes.setAntiAlias(true);
        shape_Attributes.setStyle(Paint.Style.STROKE);
        shape_Attributes.setColor(Color.BLUE);
        shape_Attributes.setStrokeWidth(5);

        selectedShape_Attributes = new Paint();
        selectedShape_Attributes.setAntiAlias(true);
        selectedShape_Attributes.setStyle(Paint.Style.STROKE);
        selectedShape_Attributes.setColor(Color.RED);
        selectedShape_Attributes.setStrokeWidth(5);
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
                    if (selectShapeEnclosingPoint(getEventPoint(event))) {
                        diagramMode = "Selection";
                    }
                } else {
                    diagramShapes.add(diagramShape);
                    diagramShapes_Path.addPath(convertShapeToPath(diagramShape));
                    handDrawnShape_Path.reset();
                }
                invalidate();
                break;
            }

            case MotionEvent.ACTION_BUTTON_PRESS: {
                Log.e("Touch Event", "Button Pressed.");
                break;
            }
        }
    }

    private void processSelectionEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mouseDragged = false;
                currentMousePosition = getEventPoint(event);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                mouseDragged = true;
                selectedShape.offset(
                        event.getX() - currentMousePosition.x,
                        event.getY() - currentMousePosition.y);
                selectedShape_Path = convertShapeToPath(selectedShape);
                currentMousePosition = getEventPoint(event);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                deselectShape();
                if (mouseDragged) {
                    diagramMode = "Drawing";
                } else {
                    if (!selectShapeEnclosingPoint(getEventPoint(event))) diagramMode = "Drawing";
                }
                invalidate();
                break;
            }
        }
    }

    private boolean selectShapeEnclosingPoint(Point point) {
        selectedShape = null;
        selectedShape_Path = null;
        for (Shape shape : diagramShapes) {
            if (shape.shapeEnclosesPoint(point)) {
                selectedShape = shape;
                diagramShapes.remove(selectedShape);
                diagramShapes_Path = convertShapesToPath(diagramShapes);
                selectedShape_Path = convertShapeToPath(selectedShape);
                break;
            }
        }
        return (selectedShape != null);
    }

    private void deselectShape() {
        if (selectedShape != null) {
            diagramShapes.add(selectedShape);
            diagramShapes_Path.addPath(convertShapeToPath(selectedShape));
            selectedShape = null;
        }
        selectedShape_Path = null;
    }

    private Path convertShapesToPath(ArrayList<Shape> shapes) {
        Path path = new Path();
        for (Shape shape : shapes) {
            path.addPath(convertShapeToPath(shape));
        }
        return path;
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

    private Point getEventPoint(MotionEvent event) {
        return new Point((int) event.getX(), (int) event.getY());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (diagramShapes_Path != null) canvas.drawPath(diagramShapes_Path, shape_Attributes);
        if (selectedShape_Path != null) canvas.drawPath(selectedShape_Path, selectedShape_Attributes);
        if (handDrawnShape_Path != null) canvas.drawPath(handDrawnShape_Path, shape_Attributes);
    }
}
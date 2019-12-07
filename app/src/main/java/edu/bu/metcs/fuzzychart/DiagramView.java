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
    String diagramMode;
    boolean mouseDragged;
    Point currentMousePosition;

    public DiagramView(Context context) {
        super(context);
        diagramMode = "Drawing";
    }

    /* ***********************************************************************
     * Define an inner class to contain the diagram shapes.
     *************************************************************************/

    private class DiagramShapes {
        ArrayList<Shape> shapes;
        Path shapes_Path;

        Shape shape;
        Path shape_Path;
        Paint shape_Attributes;

        DiagramShapes() {
            shapes = new ArrayList<>();
            shapes_Path = new Path();

            shape = new Shape();
            shape_Path = new Path();

            shape_Attributes = new Paint();
            shape_Attributes.setAntiAlias(true);
            shape_Attributes.setStyle(Paint.Style.STROKE);
            shape_Attributes.setColor(Color.BLUE);
            shape_Attributes.setStrokeWidth(5);
        }

        /* ************************************************
         * Update shape per the specified command using
         * the specified vertex.
         **************************************************/

        void updateShape(String command, Point vertex) {
            switch (command) {
                case "Start":
                    clear();
                    shape.getVertices().add(vertex);
                    shape_Path.moveTo(vertex.x, vertex.y);
                    break;
                case "Add":
                    shape.getVertices().add(vertex);
                    shape_Path.lineTo(vertex.x, vertex.y);
                    break;
            }
        }

        /* ************************************************
         * Update shape per the specified command using
         * the specified values.
         **************************************************/

         void updateShapes(String command, DiagramShapes diagramShapes) {
            updateShapes(command, diagramShapes,0, 0);
        }

        /* ************************************************
         * Update shape per the specified command using
         * the specified values.
         **************************************************/

        void updateShapes(String command, DiagramShapes diagramShapes, float x_offset, float y_offset) {
            switch (command) {
                case "Add":
                    shapes.addAll(diagramShapes.shapes);
                    shapes_Path.addPath(convertShapesToPath(diagramShapes.shapes));
                    break;
                case "Offset":
                    for (Shape shape : shapes) {
                        shape.offset(x_offset, y_offset);
                    }
                    shapes_Path = convertShapesToPath(shapes);
                    break;
            }
        }

        /* ************************************************
         * Update shapes per the specified command using
         * the specified shape.
         **************************************************/

        void updateShapes(String command, Shape shape) {
            switch (command) {
                case "Add":
                    shapes.add(shape);
                    shapes_Path.addPath(convertShapesToPath(shape));
                    break;
                case "Delete":
                    shapes.remove(shape);
                    shapes_Path = convertShapesToPath(shapes);
                    break;
            }
        }

        /* ************************************************
         * Clear the shapes and paths.
         **************************************************/

        void clear() {
            shapes.clear();
            shapes_Path.reset();
            shape.clear();
            shape_Path.reset();
        }

        /* ************************************************
         * Convert the specified shape to a path and
         * return it.
         **************************************************/

        private Path convertShapesToPath(Shape shape) {
            ArrayList<Shape> shapes = new ArrayList<>();
            shapes.add(shape);
            return convertShapesToPath(shapes);
        }

        /* ************************************************
         * Convert the specified shapes to a path and
         * return it.
         **************************************************/

        private Path convertShapesToPath(ArrayList<Shape> shapes) {
            Path path = new Path();
            Path shapePath = new Path();
            for (Shape shape : shapes) {
                shapePath.reset();
                Point startPoint = shape.getVertices().get(0);
                shapePath.moveTo(startPoint.x, startPoint.y);
                for (int pointNum = 1; pointNum < shape.getSize(); pointNum++) {
                    Point point = shape.getVertices().get(pointNum);
                    shapePath.lineTo(point.x, point.y);
                }
                path.addPath(shapePath);
            }
            return path;
        }
    }

    /* ***********************************************************************
     * Define the DiagramShapes to be filled and drawn as the user interacts
     * with the app.
     *************************************************************************/

    private class NormalShapes extends DiagramShapes {
        NormalShapes() {
            shape_Attributes.setColor(Color.BLUE);
        }
    }

    private class SelectedShapes extends DiagramShapes {
        SelectedShapes() {
            shape_Attributes.setColor(Color.RED);
        }
    }

    private class HandDrawnShapes extends DiagramShapes {
        HandDrawnShapes() {
            shape_Attributes.setColor(Color.BLACK);
        }
    }

    NormalShapes normalShapes = new NormalShapes();
    HandDrawnShapes handDrawnShapes = new HandDrawnShapes();
    SelectedShapes selectedShapes = new SelectedShapes();

    /* ***********************************************************************
     * Process a Touch event.
     *************************************************************************/

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

    /* ***********************************************************************
     * Process a Touch event in Drawing mode.
     *************************************************************************/

    private void processDrawingEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                handDrawnShapes.updateShape("Start", getEventPoint(event));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                handDrawnShapes.updateShape("Add", getEventPoint(event));
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                Shape processedShape = controller.processHandDrawnShape(handDrawnShapes.shape);
                if (processedShape.getShapeType().equals("Dot")) {
                    if (selectShapeAtPoint(getEventPoint(event))) {
                        diagramMode = "Selection";
                    }
                }
                else {
                    normalShapes.updateShapes("Add", processedShape);
                    //        handDrawnShapes.clear();
                }
                invalidate();
                break;
            }
        }
    }

    /* ***********************************************************************
     * Process a Touch event in Selection mode.
     *************************************************************************/

    private void processSelectionEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mouseDragged = false;
                currentMousePosition = getEventPoint(event);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                mouseDragged = true;
                selectedShapes.updateShapes(
                        "Offset",
                        null,
                        event.getX() - currentMousePosition.x,
                        event.getY() - currentMousePosition.y);
                currentMousePosition = getEventPoint(event);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                deselectShapes();
                if (mouseDragged) {
                    diagramMode = "Drawing";
                }
                else {
                    if (!selectShapeAtPoint(getEventPoint(event))) {
                        diagramMode = "Drawing";
                    }
                }
                invalidate();
                break;
            }
        }
    }

    /* ***********************************************************************
     * Select the shape at the specified point, if found, by moving it from
     * normalShapes to selectedShapes. Return boolean indicating if found or
     * not.
     *************************************************************************/

    private boolean selectShapeAtPoint(Point point) {
        boolean shapeSelected = false;
        for (Shape shape : normalShapes.shapes) {
            if (shape.shapeEnclosesPoint(point)) {
                selectedShapes.updateShapes("Add", shape);
                normalShapes.updateShapes("Delete", shape);
                shapeSelected = true;
                break;
            }
        }
        return shapeSelected;
    }

    /* ***********************************************************************
     * Deselect all selected shapes by moving them from selectedShapes to
     * normalShapes.
     *************************************************************************/

    private void deselectShapes() {
        normalShapes.updateShapes("Add", selectedShapes);
        selectedShapes.clear();
    }

    /* ***********************************************************************
     * Return the point from the specified MotionEvent event.
     *************************************************************************/

    private Point getEventPoint(MotionEvent event) {
        return new Point((int) event.getX(), (int) event.getY());
    }

    /* ***********************************************************************
     * Draw all normalShapes, selectedShapes, and handDrawnShapes.
     *************************************************************************/

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(handDrawnShapes.shapes_Path, handDrawnShapes.shape_Attributes);
        canvas.drawPath(handDrawnShapes.shape_Path, handDrawnShapes.shape_Attributes);

        canvas.drawPath(normalShapes.shapes_Path, normalShapes.shape_Attributes);
        canvas.drawPath(selectedShapes.shapes_Path, selectedShapes.shape_Attributes);
    }
}
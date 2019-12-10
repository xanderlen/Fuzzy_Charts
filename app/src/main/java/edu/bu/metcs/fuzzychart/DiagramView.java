package edu.bu.metcs.fuzzychart;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.text.InputType;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

public class DiagramView extends View {
    private Controller controller = Controller.getController();
    private GraphUtilities graphUtilities = GraphUtilities.getGraphUtilities();
    private String diagramMode;
    private boolean deleteMode = false;
    private boolean mouseDragged;
    private Point currentMousePosition;
    private double firstClick_StartTime = 0;
    private double secondClickStartTime = 0;
    private double thirdClickStartTime = 0;

    public DiagramView(Context context) {
        super(context);
        diagramMode = "Drawing";
    }

    /*
     * ============================================================================================
     * |                                                                                          |
     * |                              Diagram Shapes Definitions                                  |
     * |                                                                                          |
     * ============================================================================================
     */

    /* ***********************************************************************
     * Define an inner class to handle diagram shapes.
     *************************************************************************/

    private class DiagramShapes {
        ArrayList<Shape> shapes;
        Path shapes_Path;
        TextPaint shape_Attributes;

        Shape currentShape;
        Path currentShape_Path;

        DiagramShapes() {
            shapes = new ArrayList<>();
            shapes_Path = new Path();

            currentShape = new Shape();
            currentShape_Path = new Path();

            shape_Attributes = new TextPaint();
            shape_Attributes.setAntiAlias(true);
            shape_Attributes.setStyle(Paint.Style.STROKE);
            shape_Attributes.setColor(Color.BLUE);
            shape_Attributes.setStrokeWidth(3);
            shape_Attributes.setTextSize(55);
            shape_Attributes.setTextAlign(Paint.Align.CENTER);
        }

        /* ************************************************
         * Update currentShape per the specified command
         * using the specified vertex.
         **************************************************/

        void updateCurrentShape(String command, Point vertex) {
            switch (command) {
                case "New":
                    deleteCurrentShape();
                    currentShape.getVertices().add(vertex);
                    currentShape_Path.moveTo(vertex.x, vertex.y);
                    break;
                case "Add":
                    currentShape.getVertices().add(vertex);
                    currentShape_Path.lineTo(vertex.x, vertex.y);
                    break;
            }
        }

        /* ************************************************
         * Update currentShape per the specified command
         * using the specified values.
         **************************************************/

        void updateShapes(String command, DiagramShapes diagramShapes) {
            updateShapes(command, diagramShapes, 0, 0);
        }

        /* ************************************************
         * Update currentShape per the specified command
         * using the specified values.
         **************************************************/

        void updateShapes(String command, DiagramShapes diagramShapes, float x_offset, float y_offset) {
            switch (command) {
                case "Add":
                    shapes.addAll(diagramShapes.shapes);
                    shapes_Path.addPath(convertShapesToPath(diagramShapes.shapes));
                    break;
                case "Delete":
                    shapes.removeAll(diagramShapes.shapes);
                    shapes_Path = convertShapesToPath(shapes);
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
         * the specified currentShape.
         **************************************************/

        void updateShapes(String command, int shapeNum, Shape shape) {
            switch (command) {
                case "Add":
                    shapes.add(shape);
                    shapes_Path.addPath(convertShapesToPath(shape));
                    break;
                case "Replace":
                    shapes.set(shapeNum, shape);
                    shapes_Path = convertShapesToPath(shapes);
                    break;
                case "Delete":
                    shapes.remove(shape);
                    shapes_Path = convertShapesToPath(shapes);
                    break;
            }
        }

        void updateShapesText(String text, int shapeNum) {
            shapes.get(shapeNum).setShapeText(text);
        }

        /* ************************************************
         * Create new shapes and paths.
         **************************************************/

        void deleteShapes() {
            shapes = new ArrayList<>();
            shapes_Path = new Path();
            currentShape = new Shape();
            currentShape_Path = new Path();
        }

        /* ************************************************
         * Create new current shape and path.
         **************************************************/

        void deleteCurrentShape() {
            currentShape = new Shape();
            currentShape_Path = new Path();
        }
        /* ************************************************
         * Convert the specified currentShape to a path and
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

    private class Connectors extends DiagramShapes {
        ArrayList<Shape> firstConnectingShapes = new ArrayList<>();
        ArrayList<Shape> secondConnectingShapes = new ArrayList<>();

        Connectors() {
            shape_Attributes.setColor(Color.MAGENTA);
        }

        void updateConnectors(String command, int connectorNum, ArrayList<Shape> updaterShapes) {
            switch (command) {
                case "Add":
                    boolean existingConnectorFound = false;
                    for (int connectorCount = 0; connectorCount < shapes.size(); connectorCount++) {
                        if (updaterShapes.contains(firstConnectingShapes.get(connectorCount))
                                && updaterShapes.contains(secondConnectingShapes.get(connectorCount))) {
                            existingConnectorFound = true;
                            break;
                        }
                    }
                    if (!existingConnectorFound) {
                        shapes.add(new Shape());
                        firstConnectingShapes.add(updaterShapes.get(0));
                        secondConnectingShapes.add(updaterShapes.get(1));
                        reconnectConnectors(shapes.size() - 1, null);
                    }
                    break;
                case "Delete":
                    if (updaterShapes != null) {
                        updateShapes("Delete", -1, updaterShapes.get(0));
                        firstConnectingShapes.remove(updaterShapes.get(0));
                        secondConnectingShapes.remove(updaterShapes.get(0));
                    }
                    else {
                        updateShapes("Delete", -1, shapes.get(connectorNum));
                        firstConnectingShapes.remove(connectorNum);
                        secondConnectingShapes.remove(connectorNum);
                    }
                    break;
                case "Remove Broken Connectors":
                    int connectorCount = 0;
                    while (connectorCount < shapes.size()) {
                        if (!normalShapes.shapes.contains(firstConnectingShapes.get(connectorCount))
                                || !normalShapes.shapes.contains(secondConnectingShapes.get(connectorCount))) {
                            updateConnectors("Delete", connectorCount, null);
                        }
                        else {
                            connectorCount++;
                        }
                    }
                    break;
                case "Reconnect":
                    reconnectConnectors(connectorNum, updaterShapes);
                    break;
            }
        }

        private void reconnectConnectors(int connectorNum, ArrayList<Shape> connectedShapes) {
            int connectorCount_Start;
            int connectorCount_End;
            if (connectorNum < 0) {
                connectorCount_Start = 0;
                connectorCount_End = shapes.size() - 1;
            }
            else {
                connectorCount_Start = connectorNum;
                connectorCount_End = connectorNum;
            }
            for (int connectorCount = connectorCount_Start;
                 connectorCount <= connectorCount_End;
                 connectorCount++) {
                if (connectedShapes == null
                        || connectedShapes.contains(firstConnectingShapes.get(connectorCount))
                        || connectedShapes.contains(secondConnectingShapes.get(connectorCount))) {

                    Shape[] connectingShapes = new Shape[]{
                            firstConnectingShapes.get(connectorCount),
                            secondConnectingShapes.get(connectorCount)};
                    Point[] connectorPoint = new Point[]{
                            firstConnectingShapes.get(connectorCount).getCenterPoint(),
                            secondConnectingShapes.get(connectorCount).getCenterPoint()};
                    Point[] nextClosestPoint = new Point[2];

                    for (int connectorPointsAdjustmentNum = 0;
                         connectorPointsAdjustmentNum < 2;
                         connectorPointsAdjustmentNum++) {
                        for (int connectorShapeNum = 0; connectorShapeNum < 2; connectorShapeNum++) {
                            for (int nextClosestPointNum = 0; nextClosestPointNum < 2; nextClosestPointNum++) {
                                double minConnectorDistance = Double.MAX_VALUE;
                                for (Point vertex : connectingShapes[connectorShapeNum].getVertices()) {
                                    if (connectingShapes[connectorShapeNum].getShapeType().equals("Diamond")
                                            || !vertex.equals(nextClosestPoint[0])) {
                                        double connectorDistance = graphUtilities.getPointsDistance(
                                                connectorPoint[1 - connectorShapeNum],
                                                vertex);
                                        if (connectorDistance < minConnectorDistance) {
                                            minConnectorDistance = connectorDistance;
                                            nextClosestPoint[nextClosestPointNum] = vertex;
                                        }
                                    }
                                }
                            }
                            connectorPoint[connectorShapeNum] = new Point(
                                    (nextClosestPoint[0].x + nextClosestPoint[1].x) / 2,
                                    (nextClosestPoint[0].y + nextClosestPoint[1].y) / 2);
                        }
                    }
                    updateCurrentShape("New", connectorPoint[0]);
                    updateCurrentShape("Add", connectorPoint[1]);
                    double radius = 25;
                    double angle = graphUtilities.getLineDirection(connectorPoint[0], connectorPoint[1]);

                    int x = ((Double) (radius * Math.sin(Math.toRadians(25 - angle)))).intValue();
                    int y = ((Double) (radius * Math.cos(Math.toRadians(25 - angle)))).intValue();
                    Point arrowPoint = new Point(connectorPoint[1].x + x, connectorPoint[1].y + y);
                    updateCurrentShape("Add", arrowPoint);

                    x = ((Double) (radius * Math.sin(Math.toRadians(-25 - angle)))).intValue();
                    y = ((Double) (radius * Math.cos(Math.toRadians(-25 - angle)))).intValue();
                    arrowPoint = new Point(connectorPoint[1].x + x, connectorPoint[1].y + y);
                    updateCurrentShape("Add", arrowPoint);

                    arrowPoint = new Point(connectorPoint[1].x, connectorPoint[1].y);
                    updateCurrentShape("Add", arrowPoint);

                    updateShapes("Replace", connectorCount, currentShape);
                }
            }
        }
    }

    NormalShapes normalShapes = new NormalShapes();
    HandDrawnShapes handDrawnShapes = new HandDrawnShapes();
    SelectedShapes selectedShapes = new SelectedShapes();
    Connectors connectors = new Connectors();

    /*
     * ============================================================================================
     * |                                                                                          |
     * |                                 Touch Event Processing                                   |
     * |                                                                                          |
     * ============================================================================================
     */

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
     * Determine the gesture made and return it.
     *************************************************************************/

    private String getGesture(MotionEvent event) {
        String gesture = "None";
        if (event.getAction() == MotionEvent.ACTION_UP && !mouseDragged) {
            if (System.currentTimeMillis() - thirdClickStartTime <= 500) {
                gesture = "Triple Click";
            }
            else if (System.currentTimeMillis() - secondClickStartTime <= 500) {
                gesture = "Double Click";
            }
            else {
                gesture = "Click";
            }
        }
        else if (diagramMode.equals("Selection") && deleteMode) {
            Shape processedShape = controller.processHandDrawnShape(handDrawnShapes.currentShape);
            if (processedShape.getShapeType().equals("Straight Line")
                    && processedShape.shapeOverlaysShape(selectedShapes.shapes.get(0))) {
                gesture = "Strike-through";
            }
            else if (mouseDragged) {
                gesture = "Drag";
            }
        }
        else if (mouseDragged) {
            gesture = "Drag";
        }
        return gesture;
    }

    /* ***********************************************************************
     * Process a Touch event in Drawing mode.
     *************************************************************************/

    private void processDrawingEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (System.currentTimeMillis() - secondClickStartTime <= 250) {
                    thirdClickStartTime = System.currentTimeMillis();
                }
                else if (System.currentTimeMillis() - firstClick_StartTime <= 250) {
                    secondClickStartTime = System.currentTimeMillis();
                    thirdClickStartTime = 0;
                }
                else {
                    firstClick_StartTime = System.currentTimeMillis();
                    secondClickStartTime = 0;
                }
                mouseDragged = false;
                handDrawnShapes.updateCurrentShape("New", getEventPoint(event));
                break;
            case MotionEvent.ACTION_MOVE:
                mouseDragged = true;
                handDrawnShapes.updateCurrentShape("Add", getEventPoint(event));
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                switch (getGesture(event)) {
                    case "Click":
                        if (selectShapeAtPoint(getEventPoint(event))) {
                            diagramMode = "Selection";
                        }
                        break;
                    case "Double Click":
                        if (selectShapeAtPoint(getEventPoint(event))) {
                            diagramMode = "Selection";
                            enterText();
                        }
                        break;
                    case "Triple Click":
                        confirmClearDiagram();
                        break;
                    case "Drag":
                        Shape[] connectingShapes = (getConnectingShapes(handDrawnShapes.currentShape));
                        if (connectingShapes != null) {
                            connectShapes(connectingShapes);
                        }
                        else {
                            Shape processedShape = controller.processHandDrawnShape(handDrawnShapes.currentShape);
                            if (processedShape.getShapeType().equals("Dot")) {
                                if (selectShapeAtPoint(getEventPoint(event))) {
                                    diagramMode = "Selection";
                                }
                            }
                            else {
                                normalShapes.updateShapes("Add", -1, processedShape);
                            }
                        }
                        handDrawnShapes.deleteShapes();
                        break;
                }
                mouseDragged = false;
                invalidate();
                break;
        }
    }

    /* ***********************************************************************
     * Process a Touch event in Selection mode.
     *************************************************************************/

    private void processSelectionEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (System.currentTimeMillis() - firstClick_StartTime <= 250) {
                    secondClickStartTime = System.currentTimeMillis();
                }
                else {
                    firstClick_StartTime = System.currentTimeMillis();
                    secondClickStartTime = 0;
                }
                mouseDragged = false;
                deleteMode = false;
                currentMousePosition = getEventPoint(event);
                if (getShapeAtPoint(getEventPoint(event), selectedShapes) == null) {
                    deleteMode = true;
                    handDrawnShapes.updateCurrentShape("New", getEventPoint(event));
                }
                break;
            case MotionEvent.ACTION_MOVE:
                mouseDragged = true;
                if (deleteMode) {
                    handDrawnShapes.updateCurrentShape("Add", getEventPoint(event));
                }
                else {
                    selectedShapes.updateShapes(
                            "Offset",
                            null,
                            event.getX() - currentMousePosition.x,
                            event.getY() - currentMousePosition.y);
                    connectors.updateConnectors("Reconnect", -1, selectedShapes.shapes);
                    currentMousePosition = getEventPoint(event);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                switch (getGesture(event)) {
                    case "Click":
                        deselectShapes();
                        if (!selectShapeAtPoint(getEventPoint(event))) {
                            diagramMode = "Drawing";
                        }
                        break;
                    case "Double Click":
                        deselectShapes();
                        if (selectShapeAtPoint(getEventPoint(event))) {
                            enterText();
                        }
                        else {
                            diagramMode = "Drawing";
                        }
                        break;
                    case "Strike-through":
                        normalShapes.updateShapes("Delete", selectedShapes);
                        selectedShapes.deleteShapes();
                        handDrawnShapes.deleteShapes();
                        connectors.updateConnectors(
                                "Remove Broken Connectors",
                                -1,
                                null);
                        diagramMode = "Drawing";
                        break;
                    case "Drag":
                        if (handDrawnShapes.currentShape != null && handDrawnShapes.currentShape.getSize() > 0) {
                            Shape processedShape = controller.processHandDrawnShape(handDrawnShapes.currentShape);
                            handDrawnShapes.deleteShapes();
                            if (processedShape.getShapeType().equals("Dot")) {
                                deselectShapes();
                                if (!selectShapeAtPoint(getEventPoint(event))) {
                                    diagramMode = "Drawing";
                                }
                            }
                        }
                }
                deleteMode = false;
                mouseDragged = false;
                invalidate();
                break;
        }
    }

    /*
     * ============================================================================================
     * |                                                                                          |
     * |                                   Utility Functions                                      |
     * |                                                                                          |
     * ============================================================================================
     */

    /* ***********************************************************************
     * Enter text for selected shape.
     *************************************************************************/

    void enterText() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Text");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedShapes.updateShapesText(input.getText().toString(), 0);
                invalidate();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    /* ***********************************************************************
     * Enter text for selected shape.
     *************************************************************************/

    void confirmClearDiagram() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirm Clear Diagram");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                normalShapes = new NormalShapes();
                selectedShapes = new SelectedShapes();
                handDrawnShapes = new HandDrawnShapes();
                connectors = new Connectors();
                invalidate();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    /* ***********************************************************************
     * Determine if specified connectorShape connects two different shapes
     * (i.e. starts inside of one currentShape and ends at another), and if so
     * return those connecting shapes.
     *************************************************************************/

    private Shape[] getConnectingShapes(Shape connectorShape) {
        Shape[] connectingShapes = null;

        Point startPoint = connectorShape.getVertices().get(0);
        Point endPoint = connectorShape.getVertices().get(connectorShape.getSize() - 1);
        for (Shape startShape : normalShapes.shapes) {
            if (startShape.shapeEnclosesPoint(startPoint)) {
                for (Shape endShape : normalShapes.shapes) {
                    if (endShape.shapeEnclosesPoint(endPoint) && endShape != startShape) {
                        connectingShapes = new Shape[2];
                        connectingShapes[0] = startShape;
                        connectingShapes[1] = endShape;
                        break;
                    }
                }
            }
            if (connectingShapes != null) break;
        }
        return connectingShapes;
    }

    /* ***********************************************************************
     * Select the currentShape at the specified point, if found, by moving it
     * from normalShapes to selectedShapes. Return boolean indicating if found
     * or not.
     *************************************************************************/

    private boolean selectShapeAtPoint(Point point) {
        Shape shapeAtPoint = getShapeAtPoint(point, normalShapes);
        if (shapeAtPoint != null) {
            selectedShapes.updateShapes("Add", -1, shapeAtPoint);
            normalShapes.updateShapes("Delete", -1, shapeAtPoint);
        }
        return (shapeAtPoint != null);
    }

    /* ***********************************************************************
     * Deselect all selected shapes by moving them from selectedShapes to
     * normalShapes.
     *************************************************************************/

    private void deselectShapes() {
        normalShapes.updateShapes("Add", selectedShapes);
        selectedShapes.deleteShapes();
    }

    /* ***********************************************************************
     * Add a connector to connectors that connects the two specified
     * connectingShapes.
     *************************************************************************/

    private void connectShapes(Shape[] connectingShapes) {
        ArrayList<Shape> connectingShapesList = new ArrayList<>();
        connectingShapesList.add(connectingShapes[0]);
        connectingShapesList.add(connectingShapes[1]);
        connectors.updateConnectors("Add", -1, connectingShapesList);
    }

    /* ***********************************************************************
     * Return the currentShape at the specified point.
     *************************************************************************/

    private Shape getShapeAtPoint(Point point, DiagramShapes shapes) {
        Shape shapeAtPoint = null;
        for (Shape shape : shapes.shapes) {
            if (shape.shapeEnclosesPoint(point)) {
                shapeAtPoint = shape;
                break;
            }
        }
        return shapeAtPoint;
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
        canvas.drawPath(normalShapes.shapes_Path, normalShapes.shape_Attributes);
        canvas.drawPath(connectors.shapes_Path, connectors.shape_Attributes);
        canvas.drawPath(selectedShapes.shapes_Path, selectedShapes.shape_Attributes);
        canvas.drawPath(handDrawnShapes.shapes_Path, handDrawnShapes.shape_Attributes);
        canvas.drawPath(handDrawnShapes.currentShape_Path, handDrawnShapes.shape_Attributes);
        DiagramShapes diagramShapes = normalShapes;
        drawText(canvas, diagramShapes);
        diagramShapes = selectedShapes;
        drawText(canvas, diagramShapes);
    }

    void drawText(Canvas canvas, DiagramShapes diagramShapes) {
        for (int shapeNum = 0; shapeNum < diagramShapes.shapes.size(); shapeNum++) {
            if (diagramShapes.shapes.get(shapeNum).getShapeText() != null) {
                Point centerPoint = diagramShapes.shapes.get(shapeNum).getCenterPoint();
                int y_offset = 20;
                if (diagramShapes.shapes.get(shapeNum).getShapeType().equals("Triangle")) {
                    y_offset = 75;
                }
                canvas.drawText(
                        diagramShapes.shapes.get(shapeNum).getShapeText(),
                        centerPoint.x,
                        centerPoint.y + y_offset,
                        diagramShapes.shape_Attributes);
            }
        }
    }
}
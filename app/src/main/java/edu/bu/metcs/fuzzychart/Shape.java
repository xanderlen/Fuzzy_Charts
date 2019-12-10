package edu.bu.metcs.fuzzychart;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

class Shape {
    private static Shape shape;
    private String shapeType;
    private String shapeText;
    private ArrayList<Point> vertices;
    private GraphUtilities graphUtilities = GraphUtilities.getGraphUtilities();
    private final double MAX_CURVATURE_DEVIATION = 45;
    private final int MAX_CLOSED_SHAPE_END_POINTS_DISTANCE = 75;

    static Shape getShape() {
        if (shape == null) shape = new Shape();
        return shape;
    }

    Shape() {
        this.shapeType = null;
        this.shapeText = null;
        this.vertices = new ArrayList<>();
    }

    Shape(String shapeType, ArrayList<Point> vertices) {
        this.shapeType = shapeType;
        this.shapeText = null;
        this.vertices = vertices;
    }

    void setShapeText(String text) {
        this.shapeText = text;
    }

    String getShapeText() {
        return this.shapeText;
    }

    String getShapeType() {
        if (this.shapeType == null && this.vertices != null) {
            calculateShapeType();
        }
        return this.shapeType;
    }

    ArrayList<Point> getVertices() {
        return this.vertices;
    }

    int getSize() {
        return this.vertices.size();
    }

    double getCircumference() {
        double circumference = 0;
        for (int vertexNum = 1; vertexNum < getSize(); vertexNum++) {
            circumference += graphUtilities.getPointsDistance(
                    vertices.get(vertexNum - 1),
                    vertices.get(vertexNum));
        }
        return circumference;
    }

    Point getCenterPoint() {
        double min_X = findShapeMinOrMax("Min", "X");
        double max_X = findShapeMinOrMax("Max", "X");
        double min_Y = findShapeMinOrMax("Min", "Y");
        double max_Y = findShapeMinOrMax("Max", "Y");
        return new Point((int)((min_X + max_X) / 2), (int)((min_Y + max_Y) / 2));
    }

    void clear() {
        vertices.clear();
        shapeType = null;
    }

    private void calculateShapeType() {
        if (getSize() == 1) {
            shapeType = "Dot";
        }
        else if (!shapeIsClosed() && getSize() == 2) {
            shapeType = "Straight Line";
        }
        else if (!shapeIsClosed()) {
            shapeType = "Segmented Line";
        }
        else if (!shapeHasHomogeneousCurvature()) {
            shapeType = "Polygon";
        }
        else if (getSize() == 4) {
            shapeType = "Triangle";
        }
        else if (getSize() >= 8) {
            double longestSegmentLength = findShapeMinOrMax("Max", "Segment Length");
            double circumference = getCircumference();
            if (longestSegmentLength / circumference < 0.2) {
                shapeType = "Circle";
            }
            else {
                shapeType = "Polygon";
            }
        }
        else if (getSize() == 5) {
            double line1Direction = graphUtilities.getLineDirection(
                    vertices.get(0),
                    vertices.get(1));
            if ((line1Direction + 45) % 90 > 67 || (line1Direction + 45) % 90 < 23) {
                shapeType = "Diamond";
            }
            else {
                double line1Length = graphUtilities.getPointsDistance(
                        vertices.get(0),
                        vertices.get(1));
                double line2Length = graphUtilities.getPointsDistance(
                        vertices.get(1),
                        vertices.get(2));
                if (Math.min(
                        line1Length,
                        line2Length) / Math.max(line1Length,
                        line2Length) >= 0.75) {
                    shapeType = "Square";
                }
                else {
                    boolean line1IsHorizontal = (line1Direction + 90) % 180 > 135
                            || (line1Direction + 90) % 180 < 45;
                    if (line1IsHorizontal && line1Length > line2Length
                            || !line1IsHorizontal && line1Length < line2Length) {
                        shapeType = "Horizontal Rectangle";
                    }
                    else {
                        shapeType = "Vertical Rectangle";
                    }
                }
            }
        }
        else {
            shapeType = "Polygon";
        }
    }

    void offset(float x, float y) {
        for (int vertexNum = 0; vertexNum < getSize(); vertexNum++) {
            vertices.get(vertexNum).x += x;
            vertices.get(vertexNum).y += y;
        }
    }

    private boolean shapeHasHomogeneousCurvature() {
        double positiveCurvature = 0;
        double maxPositiveCurvature = 0;
        double negativeCurvature = 0;
        double maxNegativeCurvature = 0;
        double curvatureDelta;
        double segmentDirection;
        double previousSegmentDirection = -1;
        boolean hasHomogeneousCurvature = true;

        for (int vertexNum = 1; vertexNum < getSize(); vertexNum++) {
            segmentDirection = graphUtilities.getLineDirection(
                    vertices.get(vertexNum - 1),
                    vertices.get(vertexNum));

            if (previousSegmentDirection >= 0) {
                curvatureDelta = graphUtilities.getLineDirectionsDelta(
                        previousSegmentDirection,
                        segmentDirection);
                if (curvatureDelta > 0) {
                    positiveCurvature += curvatureDelta;
                    Log.e("Shape", +positiveCurvature + " <-- Positive curvature");

                    if (positiveCurvature > maxPositiveCurvature) {
                        maxPositiveCurvature = positiveCurvature;
                    }
                }
                else {
                    negativeCurvature += curvatureDelta;
                    Log.e("Shape", +negativeCurvature + " <-- Negative curvature");

                    if (negativeCurvature < maxNegativeCurvature) {
                        maxNegativeCurvature = negativeCurvature;
                    }
                }

                if (Math.min(maxPositiveCurvature, -maxNegativeCurvature) > MAX_CURVATURE_DEVIATION) {
                    hasHomogeneousCurvature = false;
                    break;
                }
            }
            previousSegmentDirection = segmentDirection;
        }
        Log.e("Shape", hasHomogeneousCurvature + " <-- Has Homogeneous Curvature");
        return hasHomogeneousCurvature;
    }

    private boolean shapeIsClosed() {
        return graphUtilities.getPointsDistance(vertices.get(0),
                vertices.get(getSize() - 1))
                <= MAX_CLOSED_SHAPE_END_POINTS_DISTANCE;
    }

    boolean shapeOverlaysShape(Shape overlaidShape) {
        Shape[] shape = {this, overlaidShape};
        double[] max_X = new double[2];
        double[] max_Y = new double[2];
        double[] min_X = new double[2];
        double[] min_Y = new double[2];

        for (int shapeNum = 0; shapeNum < 2; shapeNum++) {
            max_X[shapeNum] = shape[shapeNum].findShapeMinOrMax("Max", "X");
            max_Y[shapeNum] = shape[shapeNum].findShapeMinOrMax("Max", "Y");
            min_X[shapeNum] = shape[shapeNum].findShapeMinOrMax("Min", "X");
            min_Y[shapeNum] = shape[shapeNum].findShapeMinOrMax("Min", "Y");
        }
        int X_firstShapeNum = (max_X[0] > max_X[1]) ? 0 : 1;
        int Y_firstShapeNum = (max_Y[0] > max_Y[1]) ? 0 : 1;

        return min_X[X_firstShapeNum] <= max_X[1 - X_firstShapeNum]
            && (min_Y[Y_firstShapeNum] <= max_Y[1 - Y_firstShapeNum]);
    }

    boolean shapeEnclosesPoint(Point point) {
        return point.x >= findShapeMinOrMax("Min", "X")
                && point.x <= findShapeMinOrMax("Max", "X")
                && point.y >= findShapeMinOrMax("Min", "Y")
                && point.y <= findShapeMinOrMax("Max", "Y");
    }

    int findShapeMinOrMax(String minOrMax, String element) {
        int arraySize = getSize();
        if (element.equals("Segment Length")) {
            arraySize--;
        }
        int[] elementValues = new int[arraySize];
        int minOrMaxValue;

        Point previousVertex = new Point();
        int valueNum = 0;
        int vertexNum = 0;
        for (Point vertex : vertices) {
            switch (element) {
                case "X":
                    elementValues[valueNum] = vertex.x;
                    valueNum++;
                    break;
                case "Y":
                    elementValues[valueNum] = vertex.y;
                    valueNum++;
                    break;
                case "Segment Length":
                    if (vertexNum > 0) {
                        elementValues[valueNum] = ((Double) graphUtilities.getPointsDistance(
                                previousVertex,
                                vertex)).intValue();
                        valueNum++;
                    }
                    previousVertex = vertex;
                    break;
            }
            vertexNum++;
        }

        Arrays.sort(elementValues);
        if (minOrMax.equals("Min")) {
            minOrMaxValue = elementValues[0];
        }
        else {
            minOrMaxValue = elementValues[elementValues.length - 1];
        }
        return minOrMaxValue;
    }
}

package edu.bu.metcs.fuzzychart;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

class Shape {
    private static Shape shape;
    private String shapeType;
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
        this.vertices = new ArrayList<>();
    }

    Shape(String shapeType, ArrayList<Point> vertices) {
        this.shapeType = shapeType;
        this.vertices = vertices;
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
        else if (getSize() >= 9) {
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

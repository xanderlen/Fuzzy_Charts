package edu.bu.metcs.fuzzychart;

import android.graphics.Point;
import java.util.ArrayList;
import java.util.Arrays;

class Shape {
    private static Shape shape;
    private String shapeType;
    private ArrayList<Point> vertices;
    private GraphUtilities graphUtilities = GraphUtilities.getGraphUtilities();

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

    private void calculateShapeType() {
        boolean closedShape = graphUtilities.getPointsDistance(
                vertices.get(0),
                vertices.get(getSize() - 1)
                        <= MAX_CLOSED_SHAPE_END_POINTS_DISTANCE);

        if (getSize() == 1) {
            shapeType = "Dot";
        } else if (!closedShape && getSize() == 2) {
            shapeType = "Straight Line";
        } else if (!closedShape) {
            shapeType = "Segmented Line";
        } else if (!shapeHasHomogeneousCurvature(maxLineDirectionDeviation)) {
            shapeType = "Polygon";
        } else if (getSize() == 4) {
            shapeType = "Triangle";
        } else if (getSize() >= 10) {
            shapeType = "Circle";
        } else if (getSize() == 5) {
            double line1Direction = graphUtilities.getLineDirection(
                    vertices.get(0),
                    vertices.get(1));
            if ((line1Direction + 45) % 90 > 67 || (line1Direction + 45) % 90 < 23) {
                shapeType = "Diamond";
            } else {
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
                } else {
                    boolean line1IsHorizontal = (line1Direction + 90) % 180 > 135
                            || (line1Direction + 90) % 180 < 45;
                    if (line1IsHorizontal && line1Length > line2Length
                            || !line1IsHorizontal && line1Length < line2Length) {
                        shapeType = "Horizontal Rectangle";
                    } else {
                        shapeType = "Vertical Rectangle";
                    }
                }
            }
        } else {
            shapeType = "Polygon";
        }
    }

    int findShapeMinOrMax(String minOrMax, String element) {
        int arraySize = getSize();
        if (element.equals("Length")) {
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
                case "Length":
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
        } else {
            minOrMaxValue = elementValues[elementValues.length - 1];
        }
        return minOrMaxValue;
    }

    boolean shapeHasHomogeneousCurvature(double maxLineDirectionDeviation) {
        int initialCurvatureDirection = -2;
        double previousSegmentDirection = -1;
        boolean homogeneousCurvature = true;

        for (int vertexNum = 1; vertexNum < getSize(); vertexNum++) {
            double segmentDirection = graphUtilities.getLineDirection(
                    vertices.get(vertexNum - 1),
                    vertices.get(vertexNum));
            if (previousSegmentDirection > 0) {
                int directionComparison = graphUtilities.compareLineDirections(
                        previousSegmentDirection,
                        segmentDirection,
                        maxLineDirectionDeviation);
                if (initialCurvatureDirection < -1) {
                    initialCurvatureDirection = directionComparison;
                }
                if (directionComparison != 0 && directionComparison != initialCurvatureDirection) {
                    homogeneousCurvature = false;
                    break;
                }
            }
            previousSegmentDirection = segmentDirection;
        }
        return homogeneousCurvature;
    }

    boolean shapeEnclosesPoint(Point point) {

    }

}

package edu.bu.metcs.fuzzychart;

import android.graphics.Point;
import android.util.Log;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;

class ShapeRecognizer {
    private static ShapeRecognizer shapeRecognizer;
    GraphUtilities graphUtilities = GraphUtilities.getGraphUtilities();
    final private int INITIAL_MIN_POINTS_PER_LINE = 3;
    final private int INITIAL_MAX_LINE_DIRECTION_DEVIATION = 15;
    final private int FINAL_MIN_POINTS_PER_LINE = 6;
    final private int FINAL_MAX_LINE_DIRECTION_DEVIATION = 45;
    final private int MAX_CLOSED_SHAPE_END_POINTS_DISTANCE = 75;

    static ShapeRecognizer getShapeRecognizer() {
        if (shapeRecognizer == null) shapeRecognizer = new ShapeRecognizer();
        return shapeRecognizer;
    }

    /********************************************************************************************
     *
     *  Analyze and process the specified input shape, defined by a set of getVertices() which when
     *  connected together forms the shape, to derive and build a corresponding standard output
     *  shape (e.g. square or triangle).
     *
     *  @param shape - The input shape.
     *
     *  @return - The standard output shape.
     *
     ********************************************************************************************/
    Shape recognizeShape(Shape shape) {
        Shape simplifiedShape = simplifyShape(
                shape,
                INITIAL_MIN_POINTS_PER_LINE,
                INITIAL_MAX_LINE_DIRECTION_DEVIATION);
        Shape standardizedShape = standardizeShape(
                simplifiedShape,
                INITIAL_MAX_LINE_DIRECTION_DEVIATION);

        Log.e("Shape", "Shape Type (initial) = " + standardizedShape.getShapeType());

        if (!Arrays.asList("Dot", "Circle", "Straight Line").contains(standardizedShape.getShapeType())) {
            simplifiedShape = simplifyShape(
                    shape,
                    FINAL_MIN_POINTS_PER_LINE,
                    FINAL_MAX_LINE_DIRECTION_DEVIATION);
            standardizedShape = standardizeShape(
                    simplifiedShape,
                    FINAL_MAX_LINE_DIRECTION_DEVIATION);

            Log.e("Shape", "Shape Type (final) = " + standardizedShape.getShapeType());
        }

        return standardizedShape;
    }

    private Shape simplifyShape(Shape shape, int minPointsPerLine, double maxLineDirectionDeviation) {
        Shape simplifiedShape = new Shape();

        if (shape.getSize() <= 3) {
            simplifiedShape.getVertices().add(shape.getVertices().get(0));
        } else {
            double lineDirection = -1;
            int newSegment_NumPoints = 0;
            Point newSegment_StartPoint = null;
            double newSegment_Direction = -1;
            Point previousPoint = shape.getVertices().get(0);

            Log.e("Shape: ", "*** INPUT SHAPE ***");
            Log.e("Shape: ", "    Direction");

            /* ***********************************************************************
             * Analyze and process all points in the input shape.
             *************************************************************************/
            for (int vertexNum = 1; vertexNum < shape.getSize(); vertexNum++) {
                Point point = shape.getVertices().get(vertexNum);

                /* ************************************************
                 * Determine the line segment direction defined by
                 * the next two points of the input shape...
                 **************************************************/
                double lineSegmentDirection = graphUtilities.getLineDirection(previousPoint, point);

                /* ************************************************
                 * ...and if that direction differs from the
                 * current line direction of the recognized shape
                 * by a certain amount, then start a new segment
                 * in the recognized shape with the new direction.
                 **************************************************/
                if (graphUtilities.compareLineDirections
                        (lineSegmentDirection,
                                lineDirection,
                                maxLineDirectionDeviation) != 0) {
                    if (newSegment_NumPoints == 0) {
                        newSegment_StartPoint = previousPoint;
                        newSegment_NumPoints = 2;
                        newSegment_Direction = lineSegmentDirection;
                    } else if (graphUtilities.compareLineDirections(
                            lineSegmentDirection,
                            newSegment_Direction,
                            maxLineDirectionDeviation) != 0) {
                        newSegment_NumPoints = 2;
                        newSegment_Direction = lineSegmentDirection;
                    }

                    if (newSegment_NumPoints >= minPointsPerLine) {
                        simplifiedShape.getVertices().add(newSegment_StartPoint);
                        lineDirection = newSegment_Direction;
                        newSegment_NumPoints = 0;
                        Log.e("Shape", "        " + lineDirection + " <-- Direction change");
                    } else {
                        newSegment_NumPoints++;
                        Log.e("Shape", "        " + lineSegmentDirection + " <");
                    }

                } else {
                    newSegment_NumPoints = 0;
                    Log.e("Shape", "        " + lineSegmentDirection);
                }
                previousPoint = point;
            }

            /* ************************************************
             * Add the last point to the recognized shape.
             **************************************************/
            simplifiedShape.getVertices().add(previousPoint);
        }
        return simplifiedShape;
    }

    private Shape standardizeShape(Shape shape, double maxLineDirectionDeviation) {
        int longestSideLength = 0;
        int shortestSideLength = 0;

        String shapeType = shape.calculateShapeType(;

        if (shape.getSize() == 1) {
            shapeType = "Dot";
        } else {
            boolean closedShape = graphUtilities.getPointsDistance(
                    shape.getVertices().get(0),
                    shape.getVertices().get(shape.getSize() - 1))
                    <= MAX_CLOSED_SHAPE_END_POINTS_DISTANCE;

            boolean sameCurvature = shape.shapeHasHomogeneousCurvature(maxLineDirectionDeviation);

            if (closedShape) {
                if (sameCurvature) {
                    shapeType = shape.findClosedShapeType();
                } else {
                    shapeType = "Polygon";
                }
            } else {
                if (shape.getSize() == 2) {
                    shapeType = "Straight Line";
                } else {
                    shapeType = "Segmented Line";
                }
            }

            longestSideLength = shape.findShapeMinOrMax("Max","Length");
            shortestSideLength = shape.findShapeMinOrMax("Min","Length");
        }

        Point origin;
        int length;
        int height;

        ArrayList<Point> vertices = new ArrayList<>();

        switch (shapeType) {
            case "Dot":
                vertices.add(shape.getVertices().get(0));
                break;
            case "Circle":
                int maxX = shape.findShapeMinOrMax("Max", "X");
                int minX = shape.findShapeMinOrMax("Min", "X");
                int maxY = shape.findShapeMinOrMax("Max", "Y");
                int minY = shape.findShapeMinOrMax("Min", "Y");
                int originX = (maxX + minX) / 2;
                int originY = (maxY + minY) / 2;
                int radius = (maxX - minX + maxY - minY) / 4;

                for (int angle = 0; angle < 360; angle += 5) {
                    int x = ((Double)(radius * Math.sin(Math.toRadians(angle)))).intValue();
                    int y = ((Double)(radius * Math.cos(Math.toRadians(angle)))).intValue();
                    vertices.add(new Point(originX + x, originY + y));
                }
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Triangle":
                length = (longestSideLength + shortestSideLength) / 2;
                height = ((Double)(0.866 * length)).intValue();

                origin = new Point(
                        (shape.findShapeMinOrMax("Min", "X")
                                + shape.findShapeMinOrMax("Max", "X")) / 2,
                        shape.findShapeMinOrMax("Min", "Y"));

                vertices.add(origin);
                vertices.add(new Point(origin.x + length / 2, origin.y + height));
                vertices.add(new Point(origin.x - length / 2, origin.y + height));
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Square":
                length =  (longestSideLength + shortestSideLength) / 2;

                origin = new Point(
                        shape.findShapeMinOrMax("Min", "X"),
                        shape.findShapeMinOrMax("Min", "Y"));

                vertices.add(origin);
                vertices.add(new Point(origin.x + length, origin.y));
                vertices.add(new Point(origin.x + length, origin.y + length));
                vertices.add(new Point(origin.x, origin.y + length));
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Horizontal Rectangle":
                origin = new Point(
                        shape.findShapeMinOrMax("Min", "X"),
                        shape.findShapeMinOrMax("Min", "Y"));

                vertices.add(origin);
                vertices.add(new Point(origin.x + longestSideLength, origin.y));
                vertices.add(new Point(origin.x + longestSideLength, origin.y + shortestSideLength));
                vertices.add(new Point(origin.x, origin.y + shortestSideLength));
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Vertical Rectangle":
                origin = new Point(
                        shape.findShapeMinOrMax("Min", "X"),
                        shape.findShapeMinOrMax("Min", "Y"));

                vertices.add(origin);
                vertices.add(new Point(origin.x + shortestSideLength, origin.y));
                vertices.add(new Point(origin.x + shortestSideLength, origin.y + longestSideLength));
                vertices.add(new Point(origin.x, origin.y + longestSideLength));
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Diamond":
                length =  (longestSideLength + shortestSideLength) / 2;
                int halfDiagonal = ((Double)(0.7071 * length)).intValue();

                origin = new Point(
                        (shape.findShapeMinOrMax("Min", "X")
                                + shape.findShapeMinOrMax("Max", "X")) / 2,
                        shape.findShapeMinOrMax("Min", "Y"));

                vertices.add(origin);
                vertices.add(new Point(origin.x + halfDiagonal, origin.y + halfDiagonal));
                vertices.add(new Point(origin.x, origin.y + 2 * halfDiagonal));
                vertices.add(new Point(origin.x -halfDiagonal, origin.y + halfDiagonal));
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Polygon":
            default:
                for (Point shapeVertex : shape.getVertices()) {
                    vertices.add(new Point(shapeVertex));
                }
                break;
        }

        Shape standardizedShape = new Shape(shapeType, vertices);
        return standardizedShape;
    }
}

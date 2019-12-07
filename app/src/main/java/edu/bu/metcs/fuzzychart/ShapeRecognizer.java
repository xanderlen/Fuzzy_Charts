package edu.bu.metcs.fuzzychart;

import android.graphics.Point;
import android.util.Log;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;

class ShapeRecognizer {
    private static ShapeRecognizer shapeRecognizer;
    GraphUtilities graphUtilities = GraphUtilities.getGraphUtilities();
    final private int INITIAL_MIN_LINE_SEGMENT_LENGTH = 10;
    final private int INITIAL_MAX_LINE_DIRECTION_DEVIATION = 20;
    final private int FINAL_MIN_LINE_SEGMENT_LENGTH = 75;
    final private int FINAL_MAX_LINE_DIRECTION_DEVIATION = 45;

    static ShapeRecognizer getShapeRecognizer() {
        if (shapeRecognizer == null) shapeRecognizer = new ShapeRecognizer();
        return shapeRecognizer;
    }

    /********************************************************************************************
     *
     *  Analyze and process the specified input shape, defined by a set of getVertices() which
     *  when connected together forms the shape, to derive and build a corresponding standard
     *  output shape (e.g. square or triangle).
     *
     *  @param shape - The input shape.
     *
     *  @return - The standard output shape.
     *
     ********************************************************************************************/

    Shape recognizeShape(Shape shape) {
        Shape simplifiedShape = simplifyShape(
                shape,
                INITIAL_MIN_LINE_SEGMENT_LENGTH,
                INITIAL_MAX_LINE_DIRECTION_DEVIATION);
        Shape standardizedShape = standardizeShape(simplifiedShape, null);

        Log.e("Shape", "Shape Type (initial) = " + standardizedShape.getShapeType());

        if (!Arrays.asList("Dot", "Circle", "Straight Line").contains(standardizedShape.getShapeType())) {
            simplifiedShape = simplifyShape(
                    shape,
                    FINAL_MIN_LINE_SEGMENT_LENGTH,
                    FINAL_MAX_LINE_DIRECTION_DEVIATION);
            standardizedShape = standardizeShape(simplifiedShape, standardizedShape.getShapeType());

            Log.e("Shape", "Shape Type (final) = " + standardizedShape.getShapeType());
        }
        return standardizedShape;
    }

    private Shape simplifyShape(Shape shape, int minLineSegmentLength, double maxLineDirectionDeviation) {
        Shape simplifiedShape = new Shape();

        if (shape.getSize() <= 3) {
            simplifiedShape.getVertices().add(shape.getVertices().get(0));
        }
        else {
            double lineDirection = -1;
            double newSegment_Length;
            double newSegment_Direction;
            Point newSegment_StartPoint = null;
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
                Log.e("Shape", "        " + lineSegmentDirection);

                /* ************************************************
                 * ...and if that direction differs from the
                 * current line direction of the recognized shape
                 * by a certain amount, then start a new segment
                 * in the recognized shape with the new direction.
                 **************************************************/
                if (Math.abs(graphUtilities.getLineDirectionsDelta(lineDirection, lineSegmentDirection))
                        < maxLineDirectionDeviation)
                {
                    newSegment_StartPoint = null;
                }
                else {
                    if (newSegment_StartPoint == null) {
                        newSegment_StartPoint = previousPoint;
                        newSegment_Direction = lineSegmentDirection;
                        Log.e("Shape", "        " + lineSegmentDirection + " ### <-- New segment direction");
                    }
                    else {
                        newSegment_Direction = graphUtilities.getLineDirection(newSegment_StartPoint, point);
                        Log.e("Shape", "        " + newSegment_Direction + " <-- New segment direction");
                    }
                    newSegment_Length = graphUtilities.getPointsDistance(newSegment_StartPoint, point );

                    if (Math.abs(graphUtilities.getLineDirectionsDelta(newSegment_Direction, lineSegmentDirection))
                            < maxLineDirectionDeviation && newSegment_Length > minLineSegmentLength)
                    {
                        simplifiedShape.getVertices().add(newSegment_StartPoint);
                        lineDirection = newSegment_Direction;
                        newSegment_StartPoint = null;
                        Log.e("Shape", "        " + lineDirection + " <=========================== DIRECTION CHANGE AT ###");
                    }
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

    private Shape standardizeShape(Shape shape, String previousShapeType) {
        Point origin;
        int length;
        int height;
        int longestSideLength = 0;
        int shortestSideLength = 0;

        String shapeType = shape.getShapeType();
        if (previousShapeType != null && previousShapeType.equals("Polygon") && shapeType.equals("Circle")) {
            shapeType = "Polygon";
        }

        if (!shapeType.equals("Dot")) {
            longestSideLength = shape.findShapeMinOrMax("Max", "Segment Length");
            shortestSideLength = shape.findShapeMinOrMax("Min", "Segment Length");
        }
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
                    int x = ((Double) (radius * Math.sin(Math.toRadians(angle)))).intValue();
                    int y = ((Double) (radius * Math.cos(Math.toRadians(angle)))).intValue();
                    vertices.add(new Point(originX + x, originY + y));
                }
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Triangle":
                length = (longestSideLength + shortestSideLength) / 2;
                height = ((Double) (0.866 * length)).intValue();

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
                length = (longestSideLength + shortestSideLength) / 2;

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
                length = (longestSideLength + shortestSideLength) / 2;
                int halfDiagonal = ((Double) (0.7071 * length)).intValue();

                origin = new Point(
                        (shape.findShapeMinOrMax("Min", "X")
                                + shape.findShapeMinOrMax("Max", "X")) / 2,
                        shape.findShapeMinOrMax("Min", "Y"));

                vertices.add(origin);
                vertices.add(new Point(origin.x + halfDiagonal, origin.y + halfDiagonal));
                vertices.add(new Point(origin.x, origin.y + 2 * halfDiagonal));
                vertices.add(new Point(origin.x - halfDiagonal, origin.y + halfDiagonal));
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Polygon":
            default:
                for (Point shapeVertex : shape.getVertices()) {
                    vertices.add(new Point(shapeVertex));
                }
                break;
        }
        return new Shape(shapeType, vertices);
    }
}

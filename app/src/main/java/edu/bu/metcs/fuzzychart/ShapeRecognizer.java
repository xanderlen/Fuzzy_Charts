package edu.bu.metcs.fuzzychart;

import android.graphics.Point;
import android.util.Log;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;

class ShapeRecognizer {
    private static ShapeRecognizer shapeRecognizer;
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
     *  Analyze and process the specified input shape, defined by a set of vertices which when
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

        Log.e("Shape", "Shape Type (initial) = " + standardizedShape.shapeType);

        if (!standardizedShape.shapeType.equals("Circle")
                && !standardizedShape.shapeType.equals("Straight Line")) {
            simplifiedShape = simplifyShape(
                    shape,
                    FINAL_MIN_POINTS_PER_LINE,
                    FINAL_MAX_LINE_DIRECTION_DEVIATION);
            standardizedShape = standardizeShape(
                    simplifiedShape,
                    FINAL_MAX_LINE_DIRECTION_DEVIATION);

            Log.e("Shape", "Shape Type (final) = " + standardizedShape.shapeType);
        }

        return standardizedShape;
    }

    Shape simplifyShape(Shape shape, int minPointsPerLine, double maxLineDirectionDeviation) {
        Shape simplifiedShape = new Shape();

        if (shape.vertices.size() <= 3) {
            simplifiedShape.vertices.addAll(shape.vertices);
        } else {
            double lineDirection = -1;
            int newSegment_NumPoints = 0;
            Point newSegment_StartPoint = null;
            double newSegment_Direction = -1;
            Point previousPoint = shape.vertices.get(0);

            Log.e("Shape: ", "*** INPUT SHAPE ***");
            Log.e("Shape: ", "    Direction");

            /* ***********************************************************************
             * Analyze and process all points in the input shape.
             *************************************************************************/
            for (int vertexNum = 1; vertexNum < shape.vertices.size(); vertexNum++) {
                Point point = shape.vertices.get(vertexNum);

                /* ************************************************
                 * Determine the line segment direction defined by
                 * the next two points of the input shape...
                 **************************************************/
                double lineSegmentDirection = getLineDirection(previousPoint, point);

                /* ************************************************
                 * ...and if that direction differs from the
                 * current line direction of the recognized shape
                 * by a certain amount, then start a new segment
                 * in the recognized shape with the new direction.
                 **************************************************/
                if (compareLineDirections
                        (lineSegmentDirection,
                                lineDirection,
                                maxLineDirectionDeviation) != 0) {
                    if (newSegment_NumPoints == 0) {
                        newSegment_StartPoint = previousPoint;
                        newSegment_NumPoints = 2;
                        newSegment_Direction = lineSegmentDirection;
                    } else if (compareLineDirections(
                            lineSegmentDirection,
                            newSegment_Direction,
                            maxLineDirectionDeviation) != 0) {
                        newSegment_NumPoints = 2;
                        newSegment_Direction = lineSegmentDirection;
                    }

                    if (newSegment_NumPoints >= minPointsPerLine) {
                        simplifiedShape.vertices.add(newSegment_StartPoint);
                        lineDirection = newSegment_Direction;
                        newSegment_NumPoints = 0;
                        Log.e("Shape", "        " + String.valueOf(lineDirection) + " <-- Direction change");
                    } else {
                        newSegment_NumPoints++;
                        Log.e("Shape", "        " + String.valueOf(lineSegmentDirection) + " <");
                    }

                } else {
                    newSegment_NumPoints = 0;
                    Log.e("Shape", "        " + String.valueOf(lineSegmentDirection));
                }
                previousPoint = point;
            }

            /* ************************************************
             * Add the last point to the recognized shape.
             **************************************************/
            simplifiedShape.vertices.add(previousPoint);
        }
        return simplifiedShape;
    }

    private Shape standardizeShape(Shape shape, double maxLineDirectionDeviation) {
        boolean closedShape = getPointsDistance(
                shape.vertices.get(0),
                shape.vertices.get(shape.vertices.size() - 1))
                <= MAX_CLOSED_SHAPE_END_POINTS_DISTANCE;
        boolean sameCurvature = shapeHasHomogeneousCurvature(
                shape,
                maxLineDirectionDeviation);

        String shapeType;
        if (closedShape) {
            if (sameCurvature) {
                shapeType = getClosedShapeType(shape);
            } else {
                shapeType = "Polygon";
            }
        } else {
            if (shape.vertices.size() == 2) {
                shapeType ="Straight Line";
            } else {
                shapeType = "Segmented Line";
            }
        }

        int longestSideLength = getVerticesMinOrMax(
                shape.vertices,
                "Max",
                "Length");
        int shortestSideLength = getVerticesMinOrMax(
                shape.vertices,
                "Min",
                "Length");

        Point origin;
        int length;
        int height;

        ArrayList<Point> vertices = new ArrayList<>();

        switch (shapeType) {
            case "Circle":
                int maxX = getVerticesMinOrMax(shape.vertices, "Max", "X");
                int minX = getVerticesMinOrMax(shape.vertices, "Min", "X");
                int maxY = getVerticesMinOrMax(shape.vertices, "Max", "Y");
                int minY = getVerticesMinOrMax(shape.vertices, "Min", "Y");
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
                        (getVerticesMinOrMax(shape.vertices, "Min", "X")
                                + getVerticesMinOrMax(shape.vertices, "Max", "X")) / 2,
                        getVerticesMinOrMax(shape.vertices, "Min", "Y"));

                vertices.add(origin);
                vertices.add(new Point(origin.x + length / 2, origin.y + height));
                vertices.add(new Point(origin.x - length / 2, origin.y + height));
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Square":
                length =  (longestSideLength + shortestSideLength) / 2;

                origin = new Point(
                        getVerticesMinOrMax(shape.vertices, "Min", "X"),
                        getVerticesMinOrMax(shape.vertices, "Min", "Y"));

                vertices.add(origin);
                vertices.add(new Point(origin.x + length, origin.y));
                vertices.add(new Point(origin.x + length, origin.y + length));
                vertices.add(new Point(origin.x, origin.y + length));
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Horizontal Rectangle":
                origin = new Point(
                        getVerticesMinOrMax(shape.vertices, "Min", "X"),
                        getVerticesMinOrMax(shape.vertices, "Min", "Y"));

                vertices.add(origin);
                vertices.add(new Point(origin.x + longestSideLength, origin.y));
                vertices.add(new Point(origin.x + longestSideLength, origin.y + shortestSideLength));
                vertices.add(new Point(origin.x, origin.y + shortestSideLength));
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Vertical Rectangle":
                origin = new Point(
                        getVerticesMinOrMax(shape.vertices, "Min", "X"),
                        getVerticesMinOrMax(shape.vertices, "Min", "Y"));

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
                        (getVerticesMinOrMax(shape.vertices, "Min", "X")
                                + getVerticesMinOrMax(shape.vertices, "Max", "X")) / 2,
                        getVerticesMinOrMax(shape.vertices, "Min", "Y"));

                vertices.add(origin);
                vertices.add(new Point(origin.x + halfDiagonal, origin.y + halfDiagonal));
                vertices.add(new Point(origin.x, origin.y + 2 * halfDiagonal));
                vertices.add(new Point(origin.x -halfDiagonal, origin.y + halfDiagonal));
                vertices.add(new Point(vertices.get(0)));
                break;

            case "Polygon":
            default:
                for (Point shapeVertex : shape.vertices) {
                    vertices.add(new Point(shapeVertex));
                }
                break;
        }

        Shape standardizedShape = new Shape();
        standardizedShape.shapeType = shapeType;
        standardizedShape.vertices = vertices;

        return standardizedShape;
    }

    private String getClosedShapeType(Shape shape) {
        String shapeType;
        if (shape.vertices.size() == 4) {
            shapeType = "Triangle";
        } else if (shape.vertices.size() >= 10) {
            shapeType = "Circle";
        } else if (shape.vertices.size() == 5) {
            double line1Direction = getLineDirection(
                    shape.vertices.get(0),
                    shape.vertices.get(1));
            if ((line1Direction + 45) % 90 > 67 || (line1Direction + 45) % 90 < 23) {
                shapeType = "Diamond";
            } else {
                double line1Length = getPointsDistance(
                        shape.vertices.get(0),
                        shape.vertices.get(1));
                double line2Length = getPointsDistance(
                        shape.vertices.get(1),
                        shape.vertices.get(2));
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
        return shapeType;
    }

    private boolean shapeHasHomogeneousCurvature(Shape shape, double maxLineDirectionDeviation) {
        int initialCurvatureDirection = -2;
        double previousSegmentDirection = -1;
        boolean homogeneousCurvature = true;

        for (int vertexNum = 1; vertexNum < shape.vertices.size(); vertexNum++) {
            double segmentDirection = getLineDirection(
                    shape.vertices.get(vertexNum - 1),
                    shape.vertices.get(vertexNum));
            if (previousSegmentDirection > 0) {
                int directionComparison = compareLineDirections(
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

    private double getPointsDistance(Point point1, Point point2) {
        return  Math.sqrt(Math.pow(point2.x - point1.x, 2)
                + Math.pow(point2.y - point1.y, 2));
    }

    private double getLineDirection(Point firstPoint, Point secondPoint) {
        double x_length = secondPoint.x - firstPoint.x;
        double y_length = secondPoint.y - firstPoint.y;
        double diagonalLength = Math.sqrt(x_length * x_length + y_length * y_length);
        double direction = Math.toDegrees(Math.acos(y_length / diagonalLength)) + 180;
        if (Math.signum(x_length) >= 0) {
            direction = 360 - direction;
        }
        return direction;
    }

    private int compareLineDirections(double lineDirection1,
                                      double lineDirection2,
                                      double maxLineDirectionDeviation) {
        int comparisonResult = 0;

        if (lineDirection1 < 0 || lineDirection1 > 360 || lineDirection2 < 0 || lineDirection2 > 360) {
            if (lineDirection2 > lineDirection1) {
                comparisonResult = -1;
            } else {
                comparisonResult = 1;
            }
        } else {

            double deltaAcrossZeroPoint = Math.min(lineDirection1, lineDirection2)
                    + 360 - Math.max(lineDirection1, lineDirection2);

            if (deltaAcrossZeroPoint >= maxLineDirectionDeviation
                    && Math.abs(lineDirection1 - lineDirection2) >= maxLineDirectionDeviation) {
                if (lineDirection2 - lineDirection1 > 0) {
                    comparisonResult = 1;
                } else {
                    comparisonResult = -1;
                }
                if (deltaAcrossZeroPoint < 180) {
                    comparisonResult *= -1;
                }
            }
        }
        return comparisonResult;
    }

    private int getVerticesMinOrMax(ArrayList<Point> vertices, String minOrMax, String element) {
        int arraySize = vertices.size();
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
                        elementValues[valueNum] = ((Double)getPointsDistance(
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
}

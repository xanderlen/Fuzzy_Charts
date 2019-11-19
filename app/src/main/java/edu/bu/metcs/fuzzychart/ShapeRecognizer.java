package edu.bu.metcs.fuzzychart;

import android.graphics.Point;
import android.util.Log;
import java.lang.Math;

class ShapeRecognizer {
    private static ShapeRecognizer shapeRecognizer;

    static ShapeRecognizer getShapeRecognizer() {
        if (shapeRecognizer == null) shapeRecognizer = new ShapeRecognizer();
        return shapeRecognizer;
    }

    Shape recognizeShape(Shape shape) {
        Shape recognizedShape = new Shape();

        /**************
         * This is a comment
         ****************/
        if (shape.vertices.size() <= 3) {
            recognizedShape.vertices.addAll(shape.vertices);
        } else {
            double lineDirection;
            Point firstPoint = shape.vertices.remove(0);
            Point secondPoint = shape.vertices.remove(1);
            lineDirection = getDirection(firstPoint, secondPoint);
            recognizedShape.vertices.add(firstPoint);
            Point previousPoint = secondPoint;
            Log.e("Line Direction", String.valueOf(lineDirection));

            for (Point point : shape.vertices) {
                double lineSegmentDirection = getDirection(previousPoint, point);
                Log.e("Line Segment Direction", String.valueOf(lineSegmentDirection));
                if (Math.abs(lineSegmentDirection - lineDirection) > 45) {
                    recognizedShape.vertices.add(previousPoint);
                    Log.e("Line Direction", String.valueOf(lineSegmentDirection));
                    lineDirection = lineSegmentDirection;
                }
                previousPoint = point;
            }
            recognizedShape.vertices.add(previousPoint);
        }
        Shape temp_shape = shape;
        return recognizedShape;
    }

    private double getDirection(Point firstPoint, Point secondPoint) {
        double x_length = secondPoint.x - firstPoint.x;
        double y_length = secondPoint.y - firstPoint.y;
        double diagonalLength = Math.sqrt(x_length * x_length + y_length * y_length);
        return Math.toDegrees(Math.asin(y_length / diagonalLength));
    }
}

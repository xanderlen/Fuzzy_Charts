package edu.bu.metcs.fuzzychart;

import android.graphics.Point;

class GraphUtilities {
    private static GraphUtilities graphUtilities;

    static GraphUtilities getGraphUtilities() {
        if (graphUtilities == null) graphUtilities = new GraphUtilities();
        return graphUtilities;
    }


    double getPointsDistance(Point point1, Point point2) {
        return  Math.sqrt(Math.pow(point2.x - point1.x, 2)
                + Math.pow(point2.y - point1.y, 2));
    }

    double getLineDirection(Point firstPoint, Point secondPoint) {
        double x_length = secondPoint.x - firstPoint.x;
        double y_length = secondPoint.y - firstPoint.y;
        double diagonalLength = Math.sqrt(x_length * x_length + y_length * y_length);
        double direction = Math.toDegrees(Math.acos(y_length / diagonalLength)) + 180;
        if (Math.signum(x_length) >= 0) {
            direction = 360 - direction;
        }
        return direction;
    }

    int compareLineDirections(double lineDirection1,
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
}

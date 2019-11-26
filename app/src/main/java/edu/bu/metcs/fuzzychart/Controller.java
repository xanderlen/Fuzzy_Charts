package edu.bu.metcs.fuzzychart;

class Controller {
    private static Controller controller;
    ShapeRecognizer shapeRecognizer = ShapeRecognizer.getShapeRecognizer();

    static Controller getController() {
        if (controller == null) controller = new Controller();
        return controller;
    }

    Shape processHandDrawnShape(Shape handDrawnShape) {
        Shape standardShape = shapeRecognizer.recognizeShape(handDrawnShape);
        return standardShape;
    }
}

package edu.bu.metcs.fuzzychart;

class Controller {
    private static Controller controller;
    private ShapeRecognizer shapeRecognizer = ShapeRecognizer.getShapeRecognizer();

    static Controller getController() {
        if (controller == null) controller = new Controller();
        return controller;
    }

    Shape processHandDrawnShape(Shape handDrawnShape) {
        return shapeRecognizer.recognizeShape(handDrawnShape);
    }
}

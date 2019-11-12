package edu.bu.metcs.fuzzychart;

// import org.tensorflow.lite.TensorFlowLite;

public class SymbolRecognizer {
    // Perform shape recognition or training using TensorFlow.
    String training_model_dir = "model/";
    String training_model_base_file = "Training_Model";
    String training_model_file = training_model_dir + training_model_base_file + ".meta";
    String training_data_dir = "model_data/";
  //  String training_model_data = load_training_model_data();

    /**
     * Recognize and return the shape in the shape image stored on disk, which should be a digit from 0-1.
     * :return: The closest recognized digit.
     */
    public void recognize_shape() {

//        with tf.Session() as session:
//        // Load the trained model and have it predict the shape stored in the specified image.
//        training_model_data.restore(session, tf.train.latest_checkpoint(self.training_model_dir));
//        tf_graph = tf.get_default_graph();
//        x = tf_graph.get_tensor_by_name("x:0");
//        y = tf_graph.get_tensor_by_name("y:0");
//
//        // Open the image, convert it to greyscale, reduce its size, convert to floats and reshape
//        // its dimensions.
//        image = Image.open('shape.png').convert("L");
//        image.thumbnail([28, 28]);
//        image_data = asarray(image, dtype = "float32").reshape(1, 784);
//
//        // Predict and return the digit.
//        prediction = session.run(tf.argmax(y, 1), feed_dict = {x:image_data});
//        return str(prediction[0]);
    }
}

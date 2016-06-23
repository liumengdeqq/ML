import java.io.*;
import java.util.Arrays;
import org.apache.spark.api.java.*;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.function.*;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.api.java.function.VoidFunction;
import java.util.Iterator;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.classification.*;
import scala.Tuple2;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.util.MLUtils;
import org.apache.spark.mllib.feature.HashingTF;
import org.apache.spark.mllib.feature.IDF;
import org.apache.spark.mllib.feature.IDFModel;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS;
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics;

public class NB implements Serializable {
  public static void main(String[] args) {
    SparkConf sparkConf = new SparkConf().setAppName("Naive Bayes");
    JavaSparkContext sc = new JavaSparkContext(sparkConf);

    JavaRDD<String> chinese = sc.textFile("chinese.txt");
    JavaRDD<String> japanese = sc.textFile("japanese.txt");
    JavaRDD<String> test = sc.textFile("test.txt");

    NB obj = new NB();
    JavaRDD<LabeledPoint> cData = obj.getLabeledPoint(chinese, 1);
    JavaRDD<LabeledPoint> jData = obj.getLabeledPoint(japanese, 0);
    JavaRDD<LabeledPoint> trainingData = jData.union(cData);
    final NaiveBayesModel model = NaiveBayes.train(trainingData.rdd(), 1.0);

    JavaRDD<Double> prediction = test.map(new Function<String,Double>() {
      @Override
      public Double call(String line) {
        WordParser p = new WordParser(line);
        HashingTF t = new HashingTF();
        Vector v = t.transform(Arrays.asList(p.parse()));

        /* predict probabilities */
        System.out.println("INPUT: " + line);
        double[] probs = model.predictProbabilities(v).toArray();
        System.out.println("Japanese: " + probs[0]);
        System.out.println("Chinese: " + probs[1]);

        return model.predict(v);
      }
    });

    // JavaRDD<Vector> predictProbabilities = test.map(new Function<String,Double>() {
    //   @Override
    //   public Vector call(String line) {
    //     WordParser p = new WordParser(line);
    //     HashingTF t = new HashingTF();
    //     return model.predictProbabilities(t.transform(Arrays.asList(p.parse())));
    //   }
    // });

    obj.printPrediction(prediction);
  }

  JavaRDD<LabeledPoint> getLabeledPoint(JavaRDD<String> srdd, int label) {
    final int l = label;

    return srdd.map(new Function<String, LabeledPoint>() {
      @Override
      public LabeledPoint call(String line) {
        WordParser p = new WordParser(line);
        HashingTF t = new HashingTF();
        return new LabeledPoint(l, t.transform(Arrays.asList(p.parse())));
      }
    });
  }

  void printPrediction(JavaRDD<Double> drdd) {
    drdd.foreach(new VoidFunction<Double>() {
      public void call(Double d) {
        if (d == 1) {
          System.out.println("Prediction: Chinese\n");
        } else {
          System.out.println("Prediction: Japanese\n");
        }
      }
    });
  }
}

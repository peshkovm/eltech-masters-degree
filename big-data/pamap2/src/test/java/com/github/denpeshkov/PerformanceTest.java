package com.github.denpeshkov;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.collection.CollectionRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.condition.ConditionOp;
import org.datavec.api.transform.condition.column.NaNColumnCondition;
import org.datavec.api.transform.condition.column.StringColumnCondition;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.schema.Schema.Builder;
import org.datavec.api.writable.Writable;
import org.datavec.local.transforms.LocalTransformExecutor;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.parallelism.ParallelInference;
import org.deeplearning4j.parallelism.inference.InferenceMode;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import sun.misc.Contended;

public class PerformanceTest {
  private static final int NUM_ITERATIONS = 20;
  private static final int NUM_OF_FORKS = 2;
  private static final String RES_FILE_PATH = "pamap2/src/test/resources/res.csv";

  @Fork(NUM_OF_FORKS)
  @Warmup(iterations = NUM_ITERATIONS)
  @Measurement(iterations = NUM_ITERATIONS)
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public static class Pamap2AccelerationTest {
    @State(Scope.Thread)
    public static class ParallelInferenceState {
      @Param({"1", "2", "3", "4"})
      public static int numOfThreads;

      public @Contended ParallelInference pi;
      public @Contended DataSet testData;

      @Setup(Level.Trial)
      public void init(BenchmarkParams params) {
        final int batchSize = 150;
        final MultiLayerNetwork model = Utils.getModel();
        this.testData = Utils.getTestData();

        this.pi =
            new ParallelInference.Builder(model)
                .inferenceMode(InferenceMode.BATCHED)
                .batchLimit(batchSize)
                .workers(numOfThreads)
                .build();
      }
    }

    @Threads(1)
    @Benchmark
    public Evaluation evaluate(final ParallelInferenceState state) {
      final int numLabelClasses = 12;
      final ParallelInference pi = state.pi;
      final DataSet testData = state.testData;

      Evaluation eval = new Evaluation(numLabelClasses);
      INDArray output = pi.output(testData.getFeatures());
      eval.eval(testData.getLabels(), output);

      return eval;
    }
  }

  public static void main(String[] args) throws Exception {
    Options opt =
        new OptionsBuilder()
            .include(PerformanceTest.class.getName())
            .jvmArgsAppend("-XX:-RestrictContended")
            .syncIterations(true)
            .shouldFailOnError(true)
            .build();

    final Collection<RunResult> runResults = new Runner(opt).run();

    Files.deleteIfExists(Paths.get(RES_FILE_PATH));
    Files.createFile(Paths.get(RES_FILE_PATH));
    Files.write(
        Paths.get(RES_FILE_PATH),
        ("Id,"
                + "Mode,"
                + "Cnt,"
                + "Threads,"
                + "Score,"
                + "Error,"
                + "Units"
                + System.lineSeparator())
            .getBytes(),
        StandardOpenOption.APPEND);

    runResults.forEach(
        runResult -> {
          final String id = runResult.getParams().id();
          final Mode mode = runResult.getParams().getMode();
          final long sampleCount = runResult.getPrimaryResult().getSampleCount();
          final int threads = runResult.getParams().getThreads();
          final double score = runResult.getPrimaryResult().getScore();
          final double scoreError = runResult.getPrimaryResult().getScoreError();
          final String scoreUnit = runResult.getPrimaryResult().getScoreUnit();

          try {
            Files.write(
                Paths.get(RES_FILE_PATH),
                (id
                        + ","
                        + mode
                        + ","
                        + sampleCount
                        + ","
                        + threads
                        + ","
                        + score
                        + ","
                        + scoreError
                        + ","
                        + scoreUnit
                        + System.lineSeparator())
                    .getBytes(),
                StandardOpenOption.APPEND);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
  }

  private static class Utils {
    private static final String DATASET_PATH = "pamap2/src/main/resources/pamap2.csv";
    private static DataSet testData;
    private static MultiLayerNetwork model;

    static {
      // Build dataset schema
      final Schema inputSchema =
          new Builder()
              .addColumnDouble("timestamp")
              .addColumnsString("activityID")
              .addColumnInteger("heartRate")
              .addColumnDouble("handTemperature")
              .addColumnDouble("handAcc16_1")
              .addColumnDouble("handAcc16_2")
              .addColumnDouble("handAcc16_3")
              .addColumnDouble("handAcc6_1")
              .addColumnDouble("handAcc6_2")
              .addColumnDouble("handAcc6_3")
              .addColumnDouble("handGyro1")
              .addColumnDouble("handGyro2")
              .addColumnDouble("handGyro3")
              .addColumnDouble("handMagne1")
              .addColumnDouble("handMagne2")
              .addColumnDouble("handMagne3")
              .addColumnDouble("handOrientation1")
              .addColumnDouble("handOrientation2")
              .addColumnDouble("handOrientation3")
              .addColumnDouble("handOrientation4")
              .addColumnDouble("chestTemperature")
              .addColumnDouble("chestAcc16_1")
              .addColumnDouble("chestAcc16_2")
              .addColumnDouble("chestAcc16_3")
              .addColumnDouble("chestAcc6_1")
              .addColumnDouble("chestAcc6_2")
              .addColumnDouble("chestAcc6_3")
              .addColumnDouble("chestGyro1")
              .addColumnDouble("chestGyro2")
              .addColumnDouble("chestGyro3")
              .addColumnDouble("chestMagne1")
              .addColumnDouble("chestMagne2")
              .addColumnDouble("chestMagne3")
              .addColumnDouble("chestOrientation1")
              .addColumnDouble("chestOrientation2")
              .addColumnDouble("chestOrientation3")
              .addColumnDouble("chestOrientation4")
              .addColumnDouble("ankleTemperature")
              .addColumnDouble("ankleAcc16_1")
              .addColumnDouble("ankleAcc16_2")
              .addColumnDouble("ankleAcc16_3")
              .addColumnDouble("ankleAcc6_1")
              .addColumnDouble("ankleAcc6_2")
              .addColumnDouble("ankleAcc6_3")
              .addColumnDouble("ankleGyro1")
              .addColumnDouble("ankleGyro2")
              .addColumnDouble("ankleGyro3")
              .addColumnDouble("ankleMagne1")
              .addColumnDouble("ankleMagne2")
              .addColumnDouble("ankleMagne3")
              .addColumnDouble("ankleOrientation1")
              .addColumnDouble("ankleOrientation2")
              .addColumnDouble("ankleOrientation3")
              .addColumnDouble("ankleOrientation4")
              .build();

      // Transform dataset schema for training
      final TransformProcess tp =
          new TransformProcess.Builder(inputSchema)
              .removeColumns(
                  "timestamp",
                  "heartRate",
                  "handOrientation1",
                  "handOrientation2",
                  "handOrientation3",
                  "handOrientation4",
                  "chestOrientation1",
                  "chestOrientation2",
                  "chestOrientation3",
                  "chestOrientation4",
                  "ankleOrientation1",
                  "ankleOrientation2",
                  "ankleOrientation3",
                  "ankleOrientation4",
                  "handGyro1",
                  "handGyro2",
                  "handGyro3",
                  "chestGyro1",
                  "chestGyro2",
                  "chestGyro3",
                  "ankleGyro1",
                  "ankleGyro2",
                  "ankleGyro3")
              .filter(
                  new StringColumnCondition(
                      "activityID", ConditionOp.Equal, String.valueOf(Activity.other.id)))
              .filter(
                  new StringColumnCondition(
                      "activityID", ConditionOp.Equal, String.valueOf(Activity.watchingTV.id)))
              .filter(
                  new StringColumnCondition(
                      "activityID", ConditionOp.Equal, String.valueOf(Activity.computerWork.id)))
              .filter(
                  new StringColumnCondition(
                      "activityID", ConditionOp.Equal, String.valueOf(Activity.carDriving.id)))
              .filter(
                  new StringColumnCondition(
                      "activityID", ConditionOp.Equal, String.valueOf(Activity.foldingLaundry.id)))
              .filter(
                  new StringColumnCondition(
                      "activityID", ConditionOp.Equal, String.valueOf(Activity.houseCleaning.id)))
              .filter(
                  new StringColumnCondition(
                      "activityID", ConditionOp.Equal, String.valueOf(Activity.playingSoccer.id)))
              .stringMapTransform(
                  "activityID",
                  Stream.of(
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.lying.id), String.valueOf(0)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.sitting.id), String.valueOf(1)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.standing.id), String.valueOf(2)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.walking.id), String.valueOf(3)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.running.id), String.valueOf(4)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.cycling.id), String.valueOf(5)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.NordicWalking.id), String.valueOf(6)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.ascendingStairs.id), String.valueOf(7)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.descendingStairs.id), String.valueOf(7)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.vacuumCleaning.id), String.valueOf(9)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.ironing.id), String.valueOf(10)),
                          new AbstractMap.SimpleImmutableEntry<>(
                              String.valueOf(Activity.ropeJumping.id), String.valueOf(11)))
                      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
              .convertToInteger("activityID")
              .integerToCategorical(
                  "activityID",
                  Arrays.asList(
                      String.valueOf(0),
                      String.valueOf(1),
                      String.valueOf(2),
                      String.valueOf(3),
                      String.valueOf(4),
                      String.valueOf(5),
                      String.valueOf(6),
                      String.valueOf(7),
                      String.valueOf(8),
                      String.valueOf(9),
                      String.valueOf(10),
                      String.valueOf(11)))
              // .categoricalToOneHot("activityID")
              .filter(new NaNColumnCondition("handTemperature"))
              .filter(new NaNColumnCondition("handAcc16_1"))
              .filter(new NaNColumnCondition("handAcc16_2"))
              .filter(new NaNColumnCondition("handAcc16_3"))
              .filter(new NaNColumnCondition("handAcc6_1"))
              .filter(new NaNColumnCondition("handAcc6_2"))
              .filter(new NaNColumnCondition("handAcc6_3"))
              .filter(new NaNColumnCondition("handMagne1"))
              .filter(new NaNColumnCondition("handMagne2"))
              .filter(new NaNColumnCondition("handMagne3"))
              .filter(new NaNColumnCondition("chestTemperature"))
              .filter(new NaNColumnCondition("chestAcc16_1"))
              .filter(new NaNColumnCondition("chestAcc16_2"))
              .filter(new NaNColumnCondition("chestAcc16_3"))
              .filter(new NaNColumnCondition("chestAcc6_1"))
              .filter(new NaNColumnCondition("chestAcc6_2"))
              .filter(new NaNColumnCondition("chestAcc6_3"))
              .filter(new NaNColumnCondition("chestMagne1"))
              .filter(new NaNColumnCondition("chestMagne2"))
              .filter(new NaNColumnCondition("chestMagne3"))
              .filter(new NaNColumnCondition("ankleTemperature"))
              .filter(new NaNColumnCondition("ankleAcc16_1"))
              .filter(new NaNColumnCondition("ankleAcc16_2"))
              .filter(new NaNColumnCondition("ankleAcc16_3"))
              .filter(new NaNColumnCondition("ankleAcc6_1"))
              .filter(new NaNColumnCondition("ankleAcc6_2"))
              .filter(new NaNColumnCondition("ankleAcc6_3"))
              .filter(new NaNColumnCondition("ankleMagne1"))
              .filter(new NaNColumnCondition("ankleMagne2"))
              .filter(new NaNColumnCondition("ankleMagne3"))
              .build();

      final Schema outputSchema = tp.getFinalSchema();

      // Define input reader
      RecordReader rr = new CSVRecordReader(' ');
      try {
        rr.initialize(new FileSplit(new File(DATASET_PATH)));
      } catch (IOException | InterruptedException e) {
        throw new RuntimeException(e);
      }

      // Read dataset into list
      List<List<Writable>> originalData = new ArrayList<>();
      while (rr.hasNext()) {
        originalData.add(rr.next());
      }

      // Process data
      List<List<Writable>> processedData = LocalTransformExecutor.execute(originalData, tp);

      final int batchSize = 150;
      final int numLabelClasses = 12;

      final DataSetIterator iterator =
          new RecordReaderDataSetIterator(
              new CollectionRecordReader(processedData),
              batchSize,
              outputSchema.getIndexOfColumn("activityID"),
              numLabelClasses);
      final DataSet allData = iterator.next();
      allData.shuffle();
      final SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.65);

      final DataSet trainingData = testAndTrain.getTrain();
      final DataSet testData = testAndTrain.getTest();

      final NormalizerStandardize normalizer = new NormalizerStandardize();
      normalizer.fit(
          trainingData); // Collect the statistics (mean/stdev) from the training data. This does
      // not
      // modify the input data
      normalizer.transform(trainingData); // Apply normalization to the training data
      normalizer.transform(
          testData); // Apply normalization to the test data. This is using statistics calculated
      // from
      // the *training* set

      Utils.testData = testData;

      final int numInputs = outputSchema.numColumns() - 1;
      final int numOutputs = numLabelClasses;
      final long seed = 6;

      // Build Neural Net
      final MultiLayerConfiguration conf =
          new NeuralNetConfiguration.Builder()
              .seed(seed)
              .weightInit(
                  WeightInit
                      .XAVIER) // Weight initialization scheme to use, for initial weight values
              .activation(Activation.TANH) // Activation function
              .updater(new Sgd(0.1)) // Gradient updater configuration
              .l2(1e-4) // L2 regularization coefficient for the weights (excluding biases)
              .list()
              .layer(new DenseLayer.Builder().nIn(numInputs).nOut(3).build())
              .layer(new DenseLayer.Builder().nIn(3).nOut(3).build())
              .layer(
                  new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                      .activation(
                          Activation
                              .SOFTMAX) // Override the global TANH activation with softmax for this
                      // layer
                      .nIn(3)
                      .nOut(numOutputs)
                      .build())
              .build();

      final MultiLayerNetwork model = new MultiLayerNetwork(conf);
      model.init();
      // record score once every 100 iterations
      // model.setListeners(new ScoreIterationListener(100));

      for (int i = 0; i < 1_000; i++) {
        model.fit(trainingData);
      }

      Utils.model = model;
    }

    public static DataSet getTestData() {
      return testData;
    }

    public static MultiLayerNetwork getModel() {
      return model;
    }

    private static enum Activity {
      other(0),
      lying(1),
      sitting(2),
      standing(3),
      walking(4),
      running(5),
      cycling(6),
      NordicWalking(7),
      watchingTV(9),
      computerWork(10),
      carDriving(11),
      ascendingStairs(12),
      descendingStairs(13),
      vacuumCleaning(16),
      ironing(17),
      foldingLaundry(18),
      houseCleaning(19),
      playingSoccer(20),
      ropeJumping(24);

      public final int id;

      Activity(int id) {
        this.id = id;
      }
    }
  }
}

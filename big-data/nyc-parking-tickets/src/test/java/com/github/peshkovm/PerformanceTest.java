package com.github.peshkovm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.datavec.api.transform.transform.string.ConcatenateStringColumns;
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
import org.joda.time.DateTimeZone;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import sun.misc.Contended;

public class PerformanceTest {
  private static final int NUM_ITERATIONS = 20;
  private static final int NUM_OF_FORKS = 2;
  private static final String RES_FILE_PATH = "nyc-parking-tickets/src/test/resources/res.csv";

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

  @Fork(NUM_OF_FORKS)
  @Warmup(iterations = NUM_ITERATIONS)
  @Measurement(iterations = NUM_ITERATIONS)
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public static class NycParkingTicketsAccelerationTest {

    @Threads(1)
    @Benchmark
    public Evaluation evaluate(final ParallelInferenceState state) {
      final int numLabelClasses = Utils.getNumLabelClasses();
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
    private static final String DATASET_PATH =
        "nyc-parking-tickets/src/main/resources/nyc-parking-tickets.csv";
    private static DataSet testData;
    private static MultiLayerNetwork model;
    private static int numLabelClasses;

    static {
      // Build dataset schema
      final Schema inputSchema =
          new Builder()
              .addColumnLong("SUMMONS-NUMBER")
              .addColumnString("PLATE ID")
              .addColumnString("REGISTRATION STATE")
              .addColumnString("PLATE TYPE")
              .addColumnString("ISSUE-DATE")
              .addColumnInteger("VIOLATION-CODE")
              .addColumnString("SUMM-VEH-BODY")
              .addColumnString("SUMM-VEH-MAKE")
              .addColumnString("ISSUING-AGENCY")
              .addColumnString("STREET-CODE1")
              .addColumnString("STREET-CODE2")
              .addColumnString("STREET-CODE3")
              .addColumnDouble("VEHICLE EXPIRATION DATE")
              .addColumnInteger("VIOLATION LOCATION")
              .addColumnInteger("VIOLATION PRECINCT")
              .addColumnInteger("ISSUER PRECINCT")
              .addColumnInteger("ISSUER CODE")
              .addColumnInteger("ISSUER COMMAND")
              .addColumnInteger("ISSUER SQUAD")
              .addColumnString("VIOLATION TIME")
              .addColumnString("TIME FIRST OBSERVED")
              .addColumnString("VIOLATION COUNTY")
              .addColumnString("FRONT-OF-OPPOSITE")
              .addColumnString("HOUSE NUMBER")
              .addColumnString("STREET NAME")
              .addColumnString("INTERSECTING STREET")
              .addColumnInteger("DATE FIRST OBSERVED")
              .addColumnInteger("LAW SECTION")
              .addColumnString("SUB DIVISION")
              .addColumnString("VIOLATION LEGAL CODE")
              .addColumnString("DAYS IN EFFECT")
              .addColumnString("FROM HOURS IN EFFECT")
              .addColumnString("TO HOURS IN EFFECT")
              .addColumnString("VEHICLE COLOR")
              .addColumnInteger("UNREGISTERED VEHICLE?")
              .addColumnInteger("VEHICLE YEAR")
              .addColumnString("METER NUMBER")
              .addColumnInteger("FEET FROM")
              .addColumnString("VIOLATION POST CODE")
              .addColumnString("VIOLATION DESCRIPTION")
              .addColumnString("NO STANDING OR STOPPING VIOLATION")
              .addColumnString("HYDRANT VIOLATION")
              .addColumnString("DOUBLE PARKING VIOLATION")
              .build();

      // Transform dataset schema for training
      // Define input reader
      RecordReader rr = new CSVRecordReader(1, ',');
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

      final TransformProcess tp =
          new TransformProcess.Builder(inputSchema)
              .removeAllColumnsExceptFor(
                  "PLATE ID",
                  "ISSUE-DATE",
                  "VIOLATION-CODE",
                  "SUMM-VEH-BODY",
                  "SUMM-VEH-MAKE",
                  "STREET-CODE1",
                  "STREET-CODE2",
                  "STREET-CODE3",
                  "VIOLATION TIME",
                  "VEHICLE COLOR")
              .stringToCategorical(
                  "PLATE ID",
                  originalData.stream()
                      .map(
                          row -> {
                            final Writable vehBody = row.get(1);

                            return vehBody.toString();
                          })
                      .distinct()
                      .collect(Collectors.toList()))
              .categoricalToInteger("PLATE ID")
              .stringToCategorical(
                  "SUMM-VEH-BODY",
                  originalData.stream()
                      .map(
                          row -> {
                            final Writable vehBody = row.get(6);

                            return vehBody.toString();
                          })
                      .distinct()
                      .collect(Collectors.toList()))
              .stringToCategorical(
                  "SUMM-VEH-MAKE",
                  originalData.stream()
                      .map(
                          row -> {
                            final Writable vehMake = row.get(7);

                            return vehMake.toString();
                          })
                      .distinct()
                      .collect(Collectors.toList()))
              .stringToCategorical(
                  "VEHICLE COLOR",
                  originalData.stream()
                      .map(
                          row -> {
                            final Writable vehColor = row.get(33);

                            return vehColor.toString();
                          })
                      .distinct()
                      .collect(Collectors.toList()))
              .transform(
                  new ConcatenateStringColumns(
                      "STREET-CODE", "", "STREET-CODE1", "STREET-CODE2", "STREET-CODE3"))
              .removeColumns("STREET-CODE1", "STREET-CODE2", "STREET-CODE3")
              .convertToDouble("STREET-CODE")
              .stringMapTransform(
                  "ISSUE-DATE",
                  originalData.stream()
                      .map(row -> row.get(4).toString())
                      .distinct()
                      .map(
                          issueDate -> {
                            final String subIssueDate =
                                issueDate.substring(0, issueDate.length() - 12);

                            return new SimpleImmutableEntry<>(issueDate, subIssueDate);
                          })
                      .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))
              .transform(
                  new ConcatenateStringColumns(
                      "ISSUE-DATE-TIME", "", "ISSUE-DATE", "VIOLATION TIME"))
              .appendStringColumnTransform("ISSUE-DATE-TIME", "M")
              .removeColumns("ISSUE-DATE", "VIOLATION TIME")
              .stringToTimeTransform("ISSUE-DATE-TIME", "YYYY-MM-DD'T'HHmma", DateTimeZone.UTC)
              .categoricalToInteger("SUMM-VEH-BODY", "SUMM-VEH-MAKE", "VEHICLE COLOR")
              .build();

      final Schema outputSchema = tp.getFinalSchema();

      // Process data
      List<List<Writable>> processedData = LocalTransformExecutor.execute(originalData, tp);

      final int batchSize = 150;
      final int numLabelClasses =
          (int) processedData.stream().map(row -> row.get(0).toString()).distinct().count();

      Utils.numLabelClasses = numLabelClasses;

      final DataSetIterator iterator =
          new RecordReaderDataSetIterator(
              new CollectionRecordReader(processedData),
              batchSize,
              outputSchema.getIndexOfColumn("PLATE ID"),
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
              .layer(new DenseLayer.Builder().nIn(numInputs).nOut(5).build())
              .layer(new DenseLayer.Builder().nIn(5).nOut(5).build())
              .layer(new DenseLayer.Builder().nIn(5).nOut(5).build())
              .layer(new DenseLayer.Builder().nIn(5).nOut(5).build())
              .layer(new DenseLayer.Builder().nIn(5).nOut(5).build())
              .layer(
                  new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                      .activation(
                          Activation
                              .SOFTMAX) // Override the global TANH activation with softmax for this
                      // layer
                      .nIn(5)
                      .nOut(numOutputs)
                      .build())
              .build();

      final MultiLayerNetwork model = new MultiLayerNetwork(conf);
      model.init();
      // record score once every 100 iterations
      model.setListeners(new ScoreIterationListener(100));

      for (int i = 0; i < 1000; i++) {
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

    public static int getNumLabelClasses() {
      return numLabelClasses;
    }
  }
}

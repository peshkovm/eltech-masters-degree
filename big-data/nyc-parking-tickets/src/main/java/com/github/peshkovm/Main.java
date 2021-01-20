package com.github.peshkovm;

import java.io.File;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.collection.CollectionRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.analysis.DataAnalysis;
import org.datavec.api.transform.condition.string.StringRegexColumnCondition;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.schema.Schema.Builder;
import org.datavec.api.transform.transform.string.ConcatenateStringColumns;
import org.datavec.api.transform.ui.HtmlAnalysis;
import org.datavec.api.writable.Writable;
import org.datavec.local.transforms.AnalyzeLocal;
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
import org.nd4j.evaluation.EvaluationAveraging;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class Main {
  public static final String DATASET_PATH =
      "nyc-parking-tickets/src/main/resources/nyc-parking-tickets.csv";

  public static void main(String[] args) throws Exception {
    // Build dataset schema

    //dataset size = 200_000
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
    rr.initialize(new FileSplit(new File(DATASET_PATH)));

    // Read dataset into list
    List<List<Writable>> originalData = new ArrayList<>();
    while (rr.hasNext()) {
      originalData.add(rr.next());
    }

    AtomicInteger plateIdNum = new AtomicInteger();
    AtomicInteger summVehBodyNum = new AtomicInteger();
    AtomicInteger summVehMakeNum = new AtomicInteger();
    AtomicInteger vehColorNum = new AtomicInteger();

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
            .stringMapTransform(
                "PLATE ID",
                originalData.stream()
                    .map(row -> row.get(1).toString())
                    .distinct()
                    .map(
                        plateIdStr ->
                            new SimpleImmutableEntry<>(
                                plateIdStr, String.valueOf(plateIdNum.getAndIncrement())))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))
            //            .stringToCategorical(
            //                "SUMM-VEH-BODY",
            //                originalData.stream()
            //                    .map(
            //                        row -> {
            //                          final Writable vehBody = row.get(6);
            //
            //                          return vehBody.toString();
            //                        })
            //                    .distinct()
            //                    .collect(Collectors.toList()))
            //            .stringToCategorical(
            //                "SUMM-VEH-MAKE",
            //                originalData.stream()
            //                    .map(
            //                        row -> {
            //                          final Writable vehMake = row.get(7);
            //
            //                          return vehMake.toString();
            //                        })
            //                    .distinct()
            //                    .collect(Collectors.toList()))
            //            .stringToCategorical(
            //                "VEHICLE COLOR",
            //                originalData.stream()
            //                    .map(
            //                        row -> {
            //                          final Writable vehColor = row.get(33);
            //
            //                          return vehColor.toString();
            //                        })
            //                    .distinct()
            //                    .collect(Collectors.toList()))
            .stringMapTransform(
                "SUMM-VEH-BODY",
                originalData.stream()
                    .map(row -> row.get(6).toString())
                    .distinct()
                    .map(
                        plateIdStr ->
                            new SimpleImmutableEntry<>(
                                plateIdStr, String.valueOf(summVehBodyNum.getAndIncrement())))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))
            .convertToInteger("SUMM-VEH-BODY")
            .stringMapTransform(
                "SUMM-VEH-MAKE",
                originalData.stream()
                    .map(row -> row.get(7).toString())
                    .distinct()
                    .map(
                        plateIdStr ->
                            new SimpleImmutableEntry<>(
                                plateIdStr, String.valueOf(summVehMakeNum.getAndIncrement())))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))
            .convertToInteger("SUMM-VEH-MAKE")
            .stringMapTransform(
                "VEHICLE COLOR",
                originalData.stream()
                    .map(row -> row.get(33).toString())
                    .distinct()
                    .map(
                        plateIdStr ->
                            new SimpleImmutableEntry<>(
                                plateIdStr, String.valueOf(vehColorNum.getAndIncrement())))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))
            .convertToInteger("VEHICLE COLOR")
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
            .filter(new StringRegexColumnCondition("VIOLATION TIME", "....|"))
//            .filter(new StringRegexColumnCondition("VIOLATION TIME", "5555P"))
            .transform(
                new ConcatenateStringColumns("ISSUE-DATE-TIME", "", "ISSUE-DATE", "VIOLATION TIME"))
            .appendStringColumnTransform("ISSUE-DATE-TIME", "M")
            .removeColumns("ISSUE-DATE", "VIOLATION TIME")
            .stringToTimeTransform("ISSUE-DATE-TIME", "YYYY-MM-DD'T'HHmma", DateTimeZone.UTC)
            .build();

    final Schema outputSchema = tp.getFinalSchema();

    // Process data
    List<List<Writable>> processedData = LocalTransformExecutor.execute(originalData, tp);

    // Analyse data
    //    getAnalysis(processedData, outputSchema);

    final int batchSize = 150;
    final int numLabelClasses =
        (int) processedData.stream().map(row -> row.get(0).toString()).distinct().count();

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
        trainingData); // Collect the statistics (mean/stdev) from the training data. This does not
    // modify the input data
    normalizer.transform(trainingData); // Apply normalization to the training data
    normalizer.transform(
        testData); // Apply normalization to the test data. This is using statistics calculated from
    // the *training* set

    final int numInputs = outputSchema.numColumns() - 1;
    final int numOutputs = numLabelClasses;
    final long seed = 6;

    // Build Neural Net
    final MultiLayerConfiguration conf =
        new NeuralNetConfiguration.Builder()
            .seed(seed)
            .weightInit(
                WeightInit.XAVIER) // Weight initialization scheme to use, for initial weight values
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

    for (int i = 0; i < 2_000; i++) {
      model.fit(trainingData);
    }

    ParallelInference pi =
        new ParallelInference.Builder(model)
            // BATCHED mode is kind of optimization: if number of incoming requests is too high - PI
            // will be batching individual queries into single batch. If number of requests will be
            // low - queries will be processed without batching
            .inferenceMode(InferenceMode.BATCHED)

            // max size of batch for BATCHED mode. you should set this value with respect to your
            // environment (i.e. gpu memory amounts)
            .batchLimit(batchSize)

            // set this value to number of available computational devices, either CPUs or GPUs
            .workers(1)
            .build();

    // evaluate the model on the test set
    Evaluation eval = new Evaluation(numLabelClasses);
    INDArray output = pi.output(testData.getFeatures());
    eval.eval(testData.getLabels(), output);
    System.out.println(eval.stats());

    System.out.println("MCC: " + eval.matthewsCorrelation(EvaluationAveraging.Micro));
  }

  private static void getAnalysis(List<List<Writable>> data, Schema schema) throws Exception {
    int maxHistogramBuckets = 10;

    final CollectionRecordReader rr = new CollectionRecordReader(data);

    DataAnalysis dataAnalysis = AnalyzeLocal.analyze(schema, rr, maxHistogramBuckets);

    System.out.println(dataAnalysis);

    // We can get statistics on a per-column basis:
    // DoubleAnalysis da = (DoubleAnalysis) dataAnalysis.getColumnAnalysis("Sepal length");
    // double minValue = da.getMin();
    // double maxValue = da.getMax();
    // double mean = da.getMean();

    HtmlAnalysis.createHtmlAnalysisFile(
        dataAnalysis, new File("pamap2/src/main/resources/analysis.html"));
  }
}

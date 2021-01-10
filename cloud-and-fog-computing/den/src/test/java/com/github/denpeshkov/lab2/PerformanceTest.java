package com.github.denpeshkov.lab2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.internal.vm.annotation.Contended;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class PerformanceTest {
  private static final int SET_SIZE = 20_000;
  private static final int NUM_ITERATIONS = 20;
  private static final int NUM_OF_FORKS = 2;
  private static final String RES_FILE_PATH = "den/src/test/resources/lab2/res.csv";

  @State(Scope.Benchmark)
  public abstract static class SetState {
    @Contended protected HashMapInterface<Integer> set;
    @Contended protected AtomicInteger i;

    @Setup(Level.Trial)
    public abstract void init(BenchmarkParams params);

    @Setup(Level.Iteration)
    public abstract void prepareSet(BenchmarkParams params);
  }

  public static class FCHashSetAddState extends SetState {
    @Override
    public void init(BenchmarkParams params) {
      set =
          new FlatCombinedHashSetSingleCombiner<>(
              SET_SIZE / params.getThreads(), params.getThreads());
      i = new AtomicInteger(0);
    }

    @Override
    public void prepareSet(BenchmarkParams params) {
      set =
          new FlatCombinedHashSetSingleCombiner<>(
              SET_SIZE / params.getThreads(), params.getThreads());
      i.set(0);
    }

    @TearDown(Level.Iteration)
    public void check() {
      for (int j = 0; j < i.get(); j++) {
        if (!set.contains(j)) {
          throw new RuntimeException("synchronizedList.size()!=listSize at j = " + j);
        }
      }
    }
  }

  @Fork(NUM_OF_FORKS)
  @Warmup(iterations = NUM_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = NUM_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
  @BenchmarkMode(Mode.SingleShotTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public static class FCHashSetAccelerationTest {
    @Threads(1)
    @Warmup(batchSize = SET_SIZE)
    @Measurement(batchSize = SET_SIZE)
    @Benchmark
    public HashMapInterface<Integer> add_1_thread(final FCHashSetAddState state) {
      final HashMapInterface<Integer> set = state.set;
      final AtomicInteger i = state.i;

      set.add(i.getAndIncrement());
      return set;
    }

    @Threads(2)
    @Warmup(batchSize = SET_SIZE / 2)
    @Measurement(batchSize = SET_SIZE / 2)
    @Benchmark
    public HashMapInterface<Integer> add_2_thread(final FCHashSetAddState state) {
      final HashMapInterface<Integer> set = state.set;
      final AtomicInteger i = state.i;

      set.add(i.getAndIncrement());
      return set;
    }

    @Threads(4)
    @Warmup(batchSize = SET_SIZE / 4)
    @Measurement(batchSize = SET_SIZE / 4)
    @Benchmark
    public HashMapInterface<Integer> add_4_thread(final FCHashSetAddState state) {
      final HashMapInterface<Integer> set = state.set;
      final AtomicInteger i = state.i;

      set.add(i.getAndIncrement());
      return set;
    }

    @Threads(10)
    @Warmup(batchSize = SET_SIZE / 10)
    @Measurement(batchSize = SET_SIZE / 10)
    @Benchmark
    public HashMapInterface<Integer> add_10_thread(final FCHashSetAddState state) {
      final HashMapInterface<Integer> set = state.set;
      final AtomicInteger i = state.i;

      set.add(i.getAndIncrement());
      return set;
    }
  }

  public static void main(String[] args) throws Exception {
    Options opt =
        new OptionsBuilder()
            .include(PerformanceTest.class.getName())
            .jvmArgsAppend("-XX:-RestrictContended")
            .syncIterations(true)
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
}

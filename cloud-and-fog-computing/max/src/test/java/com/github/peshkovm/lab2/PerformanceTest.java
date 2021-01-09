package com.github.peshkovm.lab2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import jdk.internal.vm.annotation.Contended;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class PerformanceTest {
  private static final int NUM_OF_FORKS = 1;
  private static final int NUM_ITERATIONS = 1;
  private static final String RES_FILE_PATH = "max/src/test/resources/lab2/res.csv";

  @State(Scope.Benchmark)
  public abstract static class BarrierState {
    @Contended Barrier barrier;

    @Setup(Level.Iteration)
    public abstract void init(BenchmarkParams params);
  }

  public static class SimpleBarrierState extends BarrierState {

    @Override
    public void init(BenchmarkParams params) {
      this.barrier = new SimpleBarrier(params.getThreads());
    }
  }

  public static class SenseBarrierState extends BarrierState {

    @Override
    public void init(BenchmarkParams params) {
      this.barrier = new SenseBarrier(params.getThreads());
    }
  }

  public static class TreeBarrierState extends BarrierState {

    @Override
    public void init(BenchmarkParams params) {
      this.barrier = new TreeBarrier(params.getThreads(), 2);
    }
  }

  @Fork(NUM_OF_FORKS)
  @Warmup(iterations = NUM_ITERATIONS)
  @Measurement(iterations = NUM_ITERATIONS)
  @BenchmarkMode(Mode.SingleShotTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public static class SimpleBarrierAccelerationTest {

    @Threads(2)
    @Benchmark
    public void await_2_thread(final SimpleBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }

    @Threads(4)
    @Benchmark
    public void await_4_thread(final SimpleBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }

    @Threads(16)
    @Benchmark
    public void await_16_thread(final SimpleBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }

    @Threads(32)
    @Benchmark
    public void await_32_thread(final SimpleBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }
  }

  @Fork(NUM_OF_FORKS)
  @Warmup(iterations = NUM_ITERATIONS)
  @Measurement(iterations = NUM_ITERATIONS)
  @BenchmarkMode(Mode.SingleShotTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public static class SenseBarrierAccelerationTest {

    @Threads(2)
    @Benchmark
    public void await_2_thread(final SenseBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }

    @Threads(4)
    @Benchmark
    public void await_4_thread(final SenseBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }

    @Threads(16)
    @Benchmark
    public void await_16_thread(final SenseBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }

    @Threads(32)
    @Benchmark
    public void await_32_thread(final SenseBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }
  }

  @Fork(NUM_OF_FORKS)
  @Warmup(iterations = NUM_ITERATIONS)
  @Measurement(iterations = NUM_ITERATIONS)
  @BenchmarkMode(Mode.SingleShotTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public static class TreeBarrierAccelerationTest {

    @Threads(2)
    @Benchmark
    public void await_2_thread(final TreeBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }

    @Threads(4)
    @Benchmark
    public void await_4_thread(final TreeBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }

    @Threads(16)
    @Benchmark
    public void await_16_thread(final TreeBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }

    @Threads(32)
    @Benchmark
    public void await_32_thread(final TreeBarrierState state, Blackhole bh) {
      final Barrier barrier = state.barrier;

      barrier.await();
      bh.consume(barrier);
    }
  }

  public static void main(String[] args) throws Exception {
    Options opt =
        new OptionsBuilder()
            .include(PerformanceTest.class.getName())
            .jvmArgsAppend("-XX:-RestrictContended")
            .syncIterations(true)
            .timeout(TimeValue.seconds(5))
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
}

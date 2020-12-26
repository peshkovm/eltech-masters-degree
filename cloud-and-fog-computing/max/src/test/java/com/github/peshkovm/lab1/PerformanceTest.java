package com.github.peshkovm.lab1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import sun.misc.Contended;

public class PerformanceTest {
  private static final int SET_SIZE = 50_000;
  private static final int NUM_ITERATIONS = 20;
  private static final int ITERATION_TIME = 2000;
  private static final int NUM_OF_FORKS = 5;
  private static final String RES_FILE_PATH = "max/src/test/resources/res.txt";

  @State(Scope.Benchmark)
  public abstract static class SetState {
    @Contended protected Set<Integer> set;
    @Contended protected AtomicInteger i;

    @Setup(Level.Trial)
    public void init0() {
      init();
    }

    @Setup(Level.Iteration)
    public void prepareSet0() {
      prepareSet();
    }

    public abstract void init();

    public abstract void prepareSet();
  }

  public abstract static class SetAddState extends SetState {
    @Override
    public void prepareSet() {
      set.clear();
      i.set(0);
    }
  }

  public static class LockFreeListAddState extends SetAddState {
    @Override
    public void init() {
      set = new LockFreeList();
      i = new AtomicInteger(0);
    }

    @TearDown(Level.Iteration)
    public void check(BenchmarkParams params) {
      if (set.size() != i.get()) {
        throw new RuntimeException("lockFreeList.size()!=listSize");
      }
    }
  }

  public static class SynchronizedListAddState extends SetAddState {
    @Override
    public void init() {
      set = new SynchronizedList();
      i = new AtomicInteger(0);
    }

    @TearDown(Level.Iteration)
    public void check(BenchmarkParams params) {
      if (set.size() != i.get()) {
        throw new RuntimeException("synchronizedList.size()!=listSize");
      }
    }
  }

  @Fork(NUM_OF_FORKS)
  @Warmup(iterations = NUM_ITERATIONS, time = ITERATION_TIME, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = NUM_ITERATIONS, time = ITERATION_TIME, timeUnit = TimeUnit.MILLISECONDS)
  @BenchmarkMode(Mode.SingleShotTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public static class LockFreeListAccelerationTest {
    @Threads(1)
    @Warmup(batchSize = SET_SIZE)
    @Measurement(batchSize = SET_SIZE)
    @Benchmark
    public void add_1_thread(final LockFreeListAddState state, final Blackhole bh) {
      final Set<Integer> lockFreeList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(lockFreeList.add(i.getAndIncrement()));
    }

    @Threads(2)
    @Warmup(batchSize = SET_SIZE / 2)
    @Measurement(batchSize = SET_SIZE / 2)
    @Benchmark
    public void add_2_thread(final LockFreeListAddState state, final Blackhole bh) {
      final Set<Integer> lockFreeList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(lockFreeList.add(i.getAndIncrement()));
    }

    @Threads(3)
    @Warmup(batchSize = SET_SIZE / 3)
    @Measurement(batchSize = SET_SIZE / 3)
    @Benchmark
    public void add_3_thread(final LockFreeListAddState state, final Blackhole bh) {
      final Set<Integer> lockFreeList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(lockFreeList.add(i.getAndIncrement()));
    }

    @Threads(4)
    @Warmup(batchSize = SET_SIZE / 4)
    @Measurement(batchSize = SET_SIZE / 4)
    @Benchmark
    public void add_4_thread(final LockFreeListAddState state, final Blackhole bh) {
      final Set<Integer> lockFreeList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(lockFreeList.add(i.getAndIncrement()));
    }

    @Threads(50)
    @Warmup(batchSize = SET_SIZE / 50)
    @Measurement(batchSize = SET_SIZE / 50)
    @Benchmark
    public void add_50_thread(final LockFreeListAddState state, final Blackhole bh) {
      final Set<Integer> lockFreeList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(lockFreeList.add(i.getAndIncrement()));
    }
  }

  @Fork(NUM_OF_FORKS)
  @Warmup(iterations = NUM_ITERATIONS, time = ITERATION_TIME, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = NUM_ITERATIONS, time = ITERATION_TIME, timeUnit = TimeUnit.MILLISECONDS)
  @BenchmarkMode(Mode.Throughput)
  public static class LockFreeListThroughputTest {
    @Threads(1)
    @Benchmark
    public void add_1_thread(final LockFreeListAddState state, final Blackhole bh) {
      final Set<Integer> lockFreeList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(lockFreeList.add(i.getAndIncrement()));
    }

    @Threads(2)
    @Benchmark
    public void add_2_thread(final LockFreeListAddState state, final Blackhole bh) {
      final Set<Integer> lockFreeList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(lockFreeList.add(i.getAndIncrement()));
    }

    @Threads(3)
    @Benchmark
    public void add_3_thread(final LockFreeListAddState state, final Blackhole bh) {
      final Set<Integer> lockFreeList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(lockFreeList.add(i.getAndIncrement()));
    }

    @Threads(4)
    @Benchmark
    public void add_4_thread(final LockFreeListAddState state, final Blackhole bh) {
      final Set<Integer> lockFreeList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(lockFreeList.add(i.getAndIncrement()));
    }

    @Threads(50)
    @Benchmark
    public void add_50_thread(final LockFreeListAddState state, final Blackhole bh) {
      final Set<Integer> lockFreeList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(lockFreeList.add(i.getAndIncrement()));
    }
  }

  @Fork(NUM_OF_FORKS)
  @Warmup(iterations = NUM_ITERATIONS, time = ITERATION_TIME, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = NUM_ITERATIONS, time = ITERATION_TIME, timeUnit = TimeUnit.MILLISECONDS)
  @BenchmarkMode(Mode.SingleShotTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public static class SynchronizedListAccelerationTest {
    @Threads(1)
    @Warmup(batchSize = SET_SIZE)
    @Measurement(batchSize = SET_SIZE)
    @Benchmark
    public void add_1_thread(final SynchronizedListAddState state, final Blackhole bh) {
      final Set<Integer> synchronizedList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(synchronizedList.add(i.getAndIncrement()));
    }

    @Threads(2)
    @Warmup(batchSize = SET_SIZE / 2)
    @Measurement(batchSize = SET_SIZE / 2)
    @Benchmark
    public void add_2_thread(final SynchronizedListAddState state, final Blackhole bh) {
      final Set<Integer> synchronizedList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(synchronizedList.add(i.getAndIncrement()));
    }

    @Threads(3)
    @Warmup(batchSize = SET_SIZE / 3)
    @Measurement(batchSize = SET_SIZE / 3)
    @Benchmark
    public void add_3_thread(final SynchronizedListAddState state, final Blackhole bh) {
      final Set<Integer> synchronizedList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(synchronizedList.add(i.getAndIncrement()));
    }

    @Threads(4)
    @Warmup(batchSize = SET_SIZE / 4)
    @Measurement(batchSize = SET_SIZE / 4)
    @Benchmark
    public void add_4_thread(final SynchronizedListAddState state, final Blackhole bh) {
      final Set<Integer> synchronizedList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(synchronizedList.add(i.getAndIncrement()));
    }

    @Threads(50)
    @Warmup(batchSize = SET_SIZE / 50)
    @Measurement(batchSize = SET_SIZE / 50)
    @Benchmark
    public void add_50_thread(final SynchronizedListAddState state, final Blackhole bh) {
      final Set<Integer> synchronizedList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(synchronizedList.add(i.getAndIncrement()));
    }
  }

  @Fork(NUM_OF_FORKS)
  @Warmup(iterations = NUM_ITERATIONS, time = ITERATION_TIME, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = NUM_ITERATIONS, time = ITERATION_TIME, timeUnit = TimeUnit.MILLISECONDS)
  @BenchmarkMode(Mode.Throughput)
  public static class SynchronizedListThroughputTest {
    @Threads(1)
    @Benchmark
    public void add_1_thread(final SynchronizedListAddState state, final Blackhole bh) {
      final Set<Integer> synchronizedList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(synchronizedList.add(i.getAndIncrement()));
    }

    @Threads(2)
    @Benchmark
    public void add_2_thread(final SynchronizedListAddState state, final Blackhole bh) {
      final Set<Integer> synchronizedList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(synchronizedList.add(i.getAndIncrement()));
    }

    @Threads(3)
    @Benchmark
    public void add_3_thread(final SynchronizedListAddState state, final Blackhole bh) {
      final Set<Integer> synchronizedList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(synchronizedList.add(i.getAndIncrement()));
    }

    @Threads(4)
    @Benchmark
    public void add_4_thread(final SynchronizedListAddState state, final Blackhole bh) {
      final Set<Integer> synchronizedList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(synchronizedList.add(i.getAndIncrement()));
    }

    @Threads(50)
    @Benchmark
    public void add_50_thread(final SynchronizedListAddState state, final Blackhole bh) {
      final Set<Integer> synchronizedList = state.set;
      final AtomicInteger i = state.i;

      bh.consume(synchronizedList.add(i.getAndIncrement()));
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

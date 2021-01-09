package com.github.peshkovm.lab2;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class SimpleBarrierCorrectnessTest {
  static final int NUM_OF_THREADS = 100;
  static final ExecutorService executorService = Executors.newFixedThreadPool(NUM_OF_THREADS);
  static Barrier barrier;

  @Before
  public void setUp() {
    barrier = new SimpleBarrier(NUM_OF_THREADS);
  }

  @Test
  public void await() throws InterruptedException {
    final List<Callable<Void>> tasks = new ArrayList<>();
    AtomicInteger threadsCount = new AtomicInteger();
    for (int i = 0; i < NUM_OF_THREADS; i++) {
      tasks.add(
          Executors.callable(
              () -> {
                try {
                  threadsCount.getAndIncrement();
                  barrier.await();

                  assertEquals(threadsCount.get(), NUM_OF_THREADS);
                } catch (Exception e) {
                  e.printStackTrace();
                  assert false;
                }
              },
              null));
    }

    executorService
        .invokeAll(tasks)
        .forEach(
            future -> {
              try {
                future.get();
              } catch (Exception e) {
                e.printStackTrace();
                assert false;
              }
            });
  }

  @AfterClass
  public static void shutdownAndAwaitTermination() {
    executorService.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        executorService.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      executorService.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }
}

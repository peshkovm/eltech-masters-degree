package com.github.denpeshkov.lab2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class FlatCombinedHashSetSingleCombinerTest {
  static final int NUM_OF_THREADS = 100;
  static final ExecutorService executorService = Executors.newFixedThreadPool(NUM_OF_THREADS);
  static FlatCombinedHashSetSingleCombiner<Integer> hashSet;
  static CountDownLatch countDownLatch;

  @Before
  public void setUp() {
    hashSet = new FlatCombinedHashSetSingleCombiner<>(NUM_OF_THREADS, NUM_OF_THREADS);
    countDownLatch = new CountDownLatch(NUM_OF_THREADS);
  }

  @Test
  public void add() throws InterruptedException {
    final List<Callable<Void>> tasks = new ArrayList<>();

    for (int i = 0; i < NUM_OF_THREADS; i++) {
      int finalI = i;
      tasks.add(
          Executors.callable(
              () -> {
                countDownLatch.countDown();
                try {
                  countDownLatch.await();
                } catch (Exception e) {
                  e.printStackTrace();
                  assert false;
                }
                hashSet.add(finalI);
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

    for (int i = 0; i < NUM_OF_THREADS; i++) {
      assertTrue(hashSet.contains(i));
    }
  }

  @Test
  public void remove() throws InterruptedException {
    final List<Callable<Void>> tasks = new ArrayList<>();

    for (int i = 0; i < NUM_OF_THREADS; i++) {
      hashSet.add(i);
    }

    for (int i = 0; i < NUM_OF_THREADS; i++) {
      int finalI = i;
      tasks.add(
          Executors.callable(
              () -> {
                countDownLatch.countDown();
                try {
                  countDownLatch.await();
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
                hashSet.remove(finalI);
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

    for (int i = 0; i < NUM_OF_THREADS; i++) {
      assertFalse(hashSet.contains(i));
    }
  }

  @Test
  public void contains() throws InterruptedException {
    final List<Callable<Boolean>> tasks = new ArrayList<>();

    for (int i = 0; i < NUM_OF_THREADS; i++) {
      hashSet.add(i);
    }

    for (int i = 0; i < NUM_OF_THREADS; i++) {
      int finalI = i;
      tasks.add(
          () -> {
            countDownLatch.countDown();
            countDownLatch.await();
            return hashSet.contains(finalI);
          });
    }

    final boolean contains =
        executorService.invokeAll(tasks).stream()
            .map(
                future -> {
                  boolean res = false;
                  try {
                    res = future.get();
                  } catch (Exception e) {
                    e.printStackTrace();
                  }

                  return res;
                })
            .reduce((contains1, contains2) -> contains1 && contains2)
            .get();

    assertTrue(contains);
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

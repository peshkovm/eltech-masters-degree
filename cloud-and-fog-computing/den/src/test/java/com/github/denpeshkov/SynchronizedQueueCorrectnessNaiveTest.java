package com.github.denpeshkov;

import static org.junit.Assert.assertEquals;

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

public class SynchronizedQueueCorrectnessNaiveTest {
  static final int NUM_OF_THREADS = 50;
  static final ExecutorService executorService = Executors.newFixedThreadPool(NUM_OF_THREADS);
  static SynchronizedQueue<Integer> synchronizedQueue;
  static CountDownLatch countDownLatch;

  @Before
  public void setUp() {
    synchronizedQueue = new SynchronizedQueue<>();
    countDownLatch = new CountDownLatch(NUM_OF_THREADS);
  }

  @Test
  public void enq() throws InterruptedException {
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
                synchronizedQueue.enq(finalI);
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

    assertEquals(NUM_OF_THREADS, synchronizedQueue.size());
  }

  @Test
  public void deq() throws InterruptedException {
    final List<Callable<Void>> tasks = new ArrayList<>();

    for (int i = 0; i < NUM_OF_THREADS; i++) {
      synchronizedQueue.enq(i);
    }

    for (int i = 0; i < NUM_OF_THREADS; i++) {
      tasks.add(
          Executors.callable(
              () -> {
                countDownLatch.countDown();
                try {
                  countDownLatch.await();
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
                synchronizedQueue.deq();
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

    assertEquals(0, synchronizedQueue.size());
  }

  @Test
  public void clear() {
    for (int i = 0; i < NUM_OF_THREADS; i++) {
      synchronizedQueue.enq(i);
    }

    synchronizedQueue.clear();

    assertEquals(0, synchronizedQueue.size());
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

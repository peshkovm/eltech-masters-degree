package com.github.peshkovm.lab2;

import java.util.concurrent.atomic.AtomicInteger;

public class SenseBarrier implements Barrier {
  final AtomicInteger count;
  final int size;
  volatile boolean sense;
  final ThreadLocal<Boolean> threadSense;

  public SenseBarrier(int size) {
    this.count = new AtomicInteger(size);
    this.size = size;
    this.sense = false;
    this.threadSense = ThreadLocal.withInitial(() -> !sense);
  }

  @Override
  public void await() {
    final Boolean mySense = threadSense.get();
    final int position = count.getAndDecrement();

    if (position == 1) {
      count.set(size);
      sense = mySense;
    } else {
      while (sense != mySense) {}
    }
    threadSense.set(!mySense);
  }
}

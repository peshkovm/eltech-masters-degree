package com.github.peshkovm.lab2;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleBarrier implements Barrier {
  int threadsNum;
  final int size;
  final Lock lock;
  final Condition allThreadsArrived;

  public SimpleBarrier(int size) {
    this.threadsNum = 0;
    this.size = size;
    this.lock = new ReentrantLock();
    this.allThreadsArrived = lock.newCondition();
  }

  @Override
  public void await() {
    lock.lock();

    try {
      if (++threadsNum == size) {
        allThreadsArrived.signalAll();
      } else {
        while (threadsNum != size) {
          this.allThreadsArrived.await();
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      lock.unlock();
    }
  }
}

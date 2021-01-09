package com.github.peshkovm.lab2;

import java.util.concurrent.atomic.AtomicInteger;

public class TreeBarrier implements Barrier {
  final int radix;
  final Node[] leaf;
  volatile int leaves;
  final ThreadLocal<Boolean> threadSense;

  public TreeBarrier(int n, int r) {
    radix = r;
    leaves = 0;
    leaf = new Node[n / r];
    int depth = 0;
    threadSense = ThreadLocal.withInitial(() -> true);
    // compute tree depth
    while (n > 1) {
      depth++;
      n = n / r;
    }
    Node root = new Node();
    build(root, depth - 1);
  }
  // recursive tree constructor
  void build(Node parent, int depth) {
    if (depth == 0) {
      leaf[leaves++] = parent;
    } else {
      for (int i = 0; i < radix; i++) {
        Node child = new Node(parent);
        build(child, depth - 1);
      }
    }
  }

  public void await() {
    int me = ThreadID.get();
    Node myLeaf = leaf[me / radix];
    myLeaf.await();
  }

  private class Node {
    final AtomicInteger count;
    Node parent;
    volatile boolean sense;

    public Node() {
      sense = false;
      parent = null;
      count = new AtomicInteger(radix);
    }

    public Node(Node myParent) {
      this();
      parent = myParent;
    }

    public void await() {
      boolean mySense = threadSense.get();
      int position = count.getAndDecrement();
      if (position == 1) { // I’m last
        if (parent != null) { // Am I root?
          parent.await();
        }
        count.set(radix);
        sense = mySense;
      } else {
        while (sense != mySense) {}
      }
      threadSense.set(!mySense);
    }
  }

  private static class ThreadID {
    private static volatile int nextID = 0;

    private static class ThreadLocalID extends ThreadLocal<Integer> {
      protected synchronized Integer initialValue() {
        return nextID++;
      }
    }

    private static ThreadLocalID threadID = new ThreadLocalID();

    public static int get() {
      return threadID.get();
    }

    public static void set(int index) {
      threadID.set(index);
    }
  }
}

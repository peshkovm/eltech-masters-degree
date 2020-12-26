package com.github.peshkovm.lab1;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList implements Set<Integer> {
  private final Node head;
  private final Node tail;

  public LockFreeList() {
    this.tail = new Node(Integer.MAX_VALUE, new AtomicMarkableReference<>(null, false));
    this.head = new Node(Integer.MIN_VALUE, new AtomicMarkableReference<>(tail, false));
  }

  @Override
  public boolean add(Integer item) {
    while (true) {
      Window window = find(head, item);
      Node pred = window.pred, curr = window.curr;
      if (curr.item.equals(item)) {
        return false;
      } else {
        Node node = new Node(item, new AtomicMarkableReference<>(curr, false));
        if (pred.next.compareAndSet(curr, node, false, false)) {
          return true;
        }
      }
    }
  }

  @Override
  public boolean remove(Integer item) {
    boolean snip;
    for (int i = 0; i < 100_000; i++) {
      Window window = find(head, item);
      Node pred = window.pred, curr = window.curr;
      if (!curr.item.equals(item)) {
        return false;
      } else {
        Node succ = curr.next.getReference();
        snip = curr.next.attemptMark(succ, true);
        if (!snip) continue;
        pred.next.compareAndSet(curr, succ, false, false);
        return true;
      }
    }

    throw new RuntimeException("Spinlock was spinning for too long");
  }

  @Override
  public boolean contains(Integer item) {
    boolean[] marked = {false};
    Node curr = head;
    while (curr.item < item) {
      curr = curr.next.getReference();
      Node succ = curr.next.get(marked);
    }
    return (curr.item.equals(item) && !marked[0]);
  }

  @Override
  public synchronized void clear() {
    Node curr = this.head.next.getReference();

    while (curr != tail) {
      remove(curr.item);
      curr = curr.next.getReference();
    }
  }

  @Override
  public synchronized int size() {
    Node curr = this.head.next.getReference();
    int size = 0;

    while (curr != tail) {
      size++;
      curr = curr.next.getReference();
    }

    return size;
  }

  private Window find(Node head, int item) {
    Node pred = null, curr = null, succ = null;
    boolean[] marked = {false};
    boolean snip;
    retry:
    for (int i = 0; i < 100_000; i++) {
      pred = head;
      curr = pred.next.getReference();
      while (true) {
        succ = curr.next.get(marked);
        while (marked[0]) {
          snip = pred.next.compareAndSet(curr, succ, false, false);
          if (!snip) continue retry;
          curr = succ;
          succ = curr.next.get(marked);
        }
        if (curr.item >= item) return new Window(pred, curr);
        pred = curr;
        curr = succ;
      }
    }

    throw new RuntimeException("Spinlock was spinning for too long");
  }

  private static class Window {
    public Node pred, curr;

    Window(Node myPred, Node myCurr) {
      pred = myPred;
      curr = myCurr;
    }
  }

  private static class Node {
    final Integer item;
    AtomicMarkableReference<Node> next;

    Node(Integer item, AtomicMarkableReference<Node> next) {
      this.item = item;
      this.next = next;
    }
  }
}

package com.github.peshkovm.lab1;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LazyList implements Set<Integer> {
  private final Node head;
  private final Node tail;

  public LazyList() {
    this.tail = new Node(Integer.MAX_VALUE, null, false);
    this.head = new Node(Integer.MIN_VALUE, tail, false);
  }

  @Override
  public boolean add(Integer item) {
    while (true) {
      Node pred = this.head;
      Node curr = head.next;

      while (curr.item < item) {
        pred = curr;
        curr = curr.next;
      }

      try {
        pred.lock();
        curr.lock();

        if (validate(pred, curr)) {
          if (curr.item.equals(item)) {
            return false;
          } else {
            pred.next = new Node(item, curr, false);

            return true;
          }
        }
      } finally {
        curr.unlock();
        pred.unlock();
      }
    }
  }

  @Override
  public boolean remove(Integer item) {
    while (true) {
      Node pred = this.head;
      Node curr = head.next;

      while (curr.item < item) {
        pred = curr;
        curr = curr.next;
      }

      try {
        pred.lock();
        curr.lock();

        if (validate(pred, curr)) {
          if (!curr.item.equals(item)) {
            return false;
          } else {
            curr.marked = true;
            pred.next = curr.next;

            return true;
          }
        }
      } finally {
        curr.unlock();
        pred.unlock();
      }
    }
  }

  @Override
  public boolean contains(Integer item) {
    Node curr = this.head;

    while (curr.item < item) {
      curr = curr.next;
    }

    return curr.item.equals(item) && !curr.marked;
  }

  /**
   * Not synchronized.
   *
   * @return
   */
  public int size() {
    Node curr = this.head.next;
    int size = 0;

    while (curr != null) {
      curr = curr.next;
      size++;
    }

    size -= 1;

    return size;
  }

  private boolean validate(Node pred, Node curr) {
    return !pred.marked && !curr.marked && pred.next == curr;
  }

  private static class Node {
    final Integer item;
    Node next;
    final Lock lock;
    boolean marked;

    Node(Integer item, Node next, boolean marked) {
      this.item = item;
      this.lock = new ReentrantLock();
      this.next = next;
      this.marked = marked;
    }

    void lock() {
      lock.lock();
    }

    void unlock() {
      lock.unlock();
    }
  }
}

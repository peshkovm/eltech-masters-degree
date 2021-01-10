package com.github.denpeshkov.lab1;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeQueue<T> implements Pool<T> {
  private final AtomicReference<Node<T>> head;
  private final AtomicReference<Node<T>> tail;

  public LockFreeQueue() {
    Node<T> node = new Node<>();
    this.head = new AtomicReference<>(node);
    this.tail = new AtomicReference<>(node);
  }

  public void enq(T item) {
    Node<T> node = new Node<>();
    node.item = item;

    while (true) {
      Node<T> last = tail.get();
      Node<T> next = last.next.get();

      if (last == tail.get()) {
        if (next == null) {
          if (last.next.compareAndSet(null, node)) {
            tail.compareAndSet(last, node);
            return;
          }
        } else {
          tail.compareAndSet(last, next);
        }
      }
    }
  }

  public T deq() {
    while (true) {
      Node<T> first = head.get(), last = tail.get();
      Node<T> next = first.next.get();

      if (first == head.get()) {
        if (first == last) {
          if (next == null) {
            throw new IllegalArgumentException("Queue is empty");
          }
          tail.compareAndSet(last, next);
        } else if (head.compareAndSet(first, next)) {
          return next.item;
        }
      }
    }
  }

  @Override
  public synchronized void clear() {
    final int size = size();
    for (int i = 0; i < size; i++) {
      deq();
    }
  }

  @Override
  public synchronized int size() {
    Node<T> curr = head.get();
    int size = 0;

    while (curr != null) {
      size++;
      curr = curr.next.get();
    }

    return size - 1;
  }

  private static class Node<V> {
    V item;
    AtomicReference<Node<V>> next = new AtomicReference<>(null);
  }
}

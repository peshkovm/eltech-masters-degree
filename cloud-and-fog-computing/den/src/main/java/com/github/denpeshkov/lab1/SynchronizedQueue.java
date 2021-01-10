package com.github.denpeshkov.lab1;

public class SynchronizedQueue<T> implements Pool<T> {
  private Node<T> head;
  private Node<T> tail;

  public SynchronizedQueue() {
    this.head = new Node<>(null);
    this.tail = this.head;
  }

  @Override
  public synchronized void enq(T item) {
    Node<T> e = new Node<T>(item);
    tail.next = e;
    tail = e;
  }

  @Override
  public synchronized T deq() {
    T result;
    if (head.next == null) {
      throw new IllegalArgumentException("Queue is empty");
    }
    result = head.item;
    head = head.next;
    return result;
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
    Node<T> curr = head;
    int size = 0;

    while (curr != null) {
      size++;
      curr = curr.next;
    }

    return size - 1;
  }

  private static class Node<V> {
    V item;
    Node<V> next;

    public Node(V item) {
      this.item = item;
      next = null;
    }
  }
}

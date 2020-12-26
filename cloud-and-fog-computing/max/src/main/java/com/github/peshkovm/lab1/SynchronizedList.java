package com.github.peshkovm.lab1;

public class SynchronizedList implements Set<Integer> {
  private final Node head;
  private final Node tail;

  public SynchronizedList() {
    this.tail = new Node(Integer.MAX_VALUE, null);
    this.head = new Node(Integer.MIN_VALUE, tail);
  }

  @Override
  public synchronized boolean add(Integer item) {
    Node pred = this.head;
    Node curr = head.next;

    while (curr.item < item) {
      pred = curr;
      curr = curr.next;
    }

    if (curr.item.equals(item)) {
      return false;
    } else {
      pred.next = new Node(item, curr);

      return true;
    }
  }

  @Override
  public synchronized boolean remove(Integer item) {
    Node pred = this.head;
    Node curr = head.next;

    while (curr.item < item) {
      pred = curr;
      curr = curr.next;
    }

    if (!curr.item.equals(item)) {
      return false;
    } else {
      pred.next = curr.next;

      return true;
    }
  }

  @Override
  public synchronized boolean contains(Integer item) {
    Node curr = this.head;

    while (curr.item < item) {
      curr = curr.next;
    }

    return curr.item.equals(item);
  }

  @Override
  public void clear() {
    Node curr = this.head.next;

    while (curr != tail) {
      remove(curr.item);
      curr = curr.next;
    }
  }

  @Override
  public synchronized int size() {
    Node curr = this.head.next;
    int size = 0;

    while (curr != tail) {
      size++;
      curr = curr.next;
    }

    return size;
  }

  private static class Node {
    final Integer item;
    Node next;

    Node(Integer item, Node next) {
      this.item = item;
      this.next = next;
    }
  }
}

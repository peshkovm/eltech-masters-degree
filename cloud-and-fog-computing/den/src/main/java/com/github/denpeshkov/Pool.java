package com.github.denpeshkov;

public interface Pool<T> {
  void enq(T item);

  T deq();

  void clear();

  int size();
}

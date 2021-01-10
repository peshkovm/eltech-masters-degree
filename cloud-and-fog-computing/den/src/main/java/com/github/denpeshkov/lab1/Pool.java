package com.github.denpeshkov.lab1;

public interface Pool<T> {
  void enq(T item);

  T deq();

  void clear();

  int size();
}

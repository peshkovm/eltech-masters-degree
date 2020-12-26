package com.github.peshkovm.lab1;

public interface Set<T> {
  boolean add(T item);

  boolean remove(T item);

  boolean contains(T item);

  void clear();

  int size();
}

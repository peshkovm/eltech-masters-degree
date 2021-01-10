package com.github.denpeshkov.lab2;

import java.util.concurrent.locks.Lock;

public interface HashMapInterface<T> {

  public boolean add(T x);

  public boolean remove(T x);

  public boolean contains(T x);
}

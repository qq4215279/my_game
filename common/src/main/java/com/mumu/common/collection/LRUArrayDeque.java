/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.collection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * LRUArrayDeque
 * 固定大小并且遵循先进先出（FIFO）容器
 * 注：存在线程安全问题
 * @author liuzhen
 * @version 1.0.0 2025/3/14 13:57
 */
public class LRUArrayDeque<E> {
  /** 长度 */
  private final int maxSize;
  /** 双端队列 */
  private final Deque<E> deque;

  public LRUArrayDeque(int maxSize) {
    this.maxSize = maxSize;
    this.deque = new ArrayDeque<>(maxSize);
  }

  public E get(int index) {
    if (index < 0 || index >= deque.size()) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + deque.size());
    }
    return (E) deque.toArray()[index];
  }

  public List<E> getAllElements() {
    // 将 deque 中的元素添加到新的 ArrayList 中
    return new ArrayList<>(deque);
  }

  public void add(E element) {
    // 移除队列头部的元素，即最老的元素
    if (deque.size() >= maxSize) {
      deque.poll();
    }
    deque.offer(element);
  }

  public void addFirst(E element) {
    if (deque.size() >= maxSize) {
      // 如果达到最大容量，移除尾部元素
      deque.pollLast();
    }
    deque.offerFirst(element);
  }

  public void addLast(E element) {
    // 如果达到最大容量，移除头部元素
    if (deque.size() >= maxSize) {
      deque.poll();
    }
    deque.offerLast(element);
  }

  public boolean remove(Object o) {
    return deque.remove(o);
  }

  public E removeFirst() {
    // 移除并返回头部元素
    return deque.pollFirst();
  }

  public E removeLast() {
    // 移除并返回尾部元素
    return deque.pollLast();
  }

  public int size() {
    return deque.size();
  }

  public boolean isEmpty() {
    return deque.isEmpty();
  }

  boolean contains(Object o) {
    return deque.contains(o);
  }

  @Override
  public String toString() {
    return deque.toString();
  }

  public static void main(String[] args) {
    LRUArrayDeque<String> fifoList = new LRUArrayDeque<>(3);

    fifoList.add("One");
    fifoList.add("Two");
    fifoList.add("Three");

    System.out.println(fifoList); // [One, Two, Three]

    fifoList.add("Four");
    System.out.println(fifoList); // [Two, Three, Four]

    fifoList.add("Five");
    System.out.println(fifoList); // [Three, Four, Five]

    System.out.println("Removed first: " + fifoList.removeFirst()); // Removes "Three"
    System.out.println(fifoList); // [Four, Five]

    System.out.println("Removed last: " + fifoList.removeLast()); // Removes "Five"
    System.out.println(fifoList); // [Four]

    fifoList.addFirst("Zero");
    System.out.println(fifoList); // [Zero, Four]

    fifoList.addLast("LAST");
    System.out.println(fifoList); // [Zero, Four, LAST]
  }

}

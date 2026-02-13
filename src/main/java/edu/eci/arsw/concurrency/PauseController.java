package edu.eci.arsw.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class PauseController {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition unpaused = lock.newCondition();
  private final Condition allPaused = lock.newCondition();
  private volatile boolean paused = false;
  private int activeThreads = 0;
  private int pausedThreads = 0;

  public void registerThread() {
    lock.lock();
    try {
      activeThreads++;
    } finally {
      lock.unlock();
    }
  }

  public void unregisterThread() {
    lock.lock();
    try {
      activeThreads--;
    } finally {
      lock.unlock();
    }
  }

  public void pause() {
    lock.lock();
    try {
      paused = true;
    } finally {
      lock.unlock();
    }
  }

  public void pauseAndWaitAll() throws InterruptedException {
    lock.lock();
    try {
      paused = true;
      while (pausedThreads < activeThreads) {
        allPaused.await();
      }
    } finally {
      lock.unlock();
    }
  }

  public void resume() {
    lock.lock();
    try {
      paused = false;
      pausedThreads = 0;
      unpaused.signalAll();
    } finally {
      lock.unlock();
    }
  }

  public boolean paused() { return paused; }

  public void awaitIfPaused() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      if (paused) {
        pausedThreads++;
        allPaused.signalAll();
        while (paused) {
          unpaused.await();
        }
        pausedThreads--;
      }
    } finally {
      lock.unlock();
    }
  }
}

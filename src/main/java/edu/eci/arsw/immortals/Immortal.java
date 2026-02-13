package edu.eci.arsw.immortals;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import edu.eci.arsw.concurrency.PauseController;

public final class Immortal implements Runnable {
  private final String name;
  private AtomicInteger health;
  private final int damage;
  private final CopyOnWriteArrayList<Immortal> population;
  private final ScoreBoard scoreBoard;
  private final PauseController controller;
  private volatile boolean running = true;
  private ReentrantLock lock = new ReentrantLock();



  public Immortal(String name, AtomicInteger health, int damage, CopyOnWriteArrayList<Immortal> population, ScoreBoard scoreBoard, PauseController controller) {
    this.name = Objects.requireNonNull(name);
    this.health = health;
    this.damage = damage;
    this.population = Objects.requireNonNull(population);
    this.scoreBoard = Objects.requireNonNull(scoreBoard);
    this.controller = Objects.requireNonNull(controller);
  }

  public String name() { return name; }
  public  int getHealth() { return health.get(); }
  public boolean isAlive() { return getHealth() > 0 && running; }
  public void stop() { running = false; }
  public void setRunning(boolean running) { this.running = running; }
  @Override public void run() {
    controller.registerThread();
    try {
      while (running) {
        controller.awaitIfPaused();

        if (!running) break;
        var opponent = pickOpponent();
        if (opponent == null) continue;
        String mode = System.getProperty("fight", "ordered");
        if ("naive".equalsIgnoreCase(mode)) fightNaive(opponent);
        else fightOrdered(opponent);
        Thread.sleep(2);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    } finally {
      controller.unregisterThread();
    }
  }

  private Immortal pickOpponent() {
    if (population.size() <= 1) return null;
    Immortal other;
    do {
      other = population.get(ThreadLocalRandom.current().nextInt(population.size()));
    } while (other == this);
    return other;
  }

  public void setHealth(int health) { this.health.set(health); }

  private void fightNaive(Immortal other) throws InterruptedException {

    if((lock.tryLock(1000, TimeUnit.MILLISECONDS)) && (other.lock.tryLock(1000, TimeUnit.MILLISECONDS))){
      try{
        if (this.getHealth() <= 0 || other.getHealth() <= 0) return;
        int saludOponenteAntes = other.getHealth();
        int dañoReal = this.damage;

        other.setHealth(saludOponenteAntes - dañoReal);
        this.setHealth(this.getHealth() + (dañoReal / 2));
        scoreBoard.recordFight();
      }finally{
        lock.unlock();
        other.lock.unlock();
      }
    }else{
      if(lock.isHeldByCurrentThread()) lock.unlock();
      if(other.lock.isHeldByCurrentThread()) other.lock.unlock();
    }

  }

  private void fightOrdered(Immortal other) {
    Immortal first = this.name.compareTo(other.name) < 0 ? this : other;
    Immortal second = this.name.compareTo(other.name) < 0 ? other : this;
    synchronized (first) {
      synchronized (second) {
        if (this.getHealth() <= 0 || other.getHealth() <= 0) return;

        int saludOponenteAntes = other.getHealth();
        int dañoReal = this.damage;

        other.setHealth(saludOponenteAntes - dañoReal);
        this.setHealth(this.getHealth() + (dañoReal / 2));

        scoreBoard.recordFight();
      }
    }
  }
}

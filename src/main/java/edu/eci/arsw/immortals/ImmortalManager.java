package edu.eci.arsw.immortals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import edu.eci.arsw.concurrency.PauseController;

public final class ImmortalManager implements AutoCloseable {
  private final CopyOnWriteArrayList<Immortal> population = new CopyOnWriteArrayList<>();
  private final List<Future<?>> futures = new ArrayList<>();
  private final PauseController controller = new PauseController();
  private final ScoreBoard scoreBoard = new ScoreBoard();
  private ExecutorService exec;
  private final int initialPopulationSize;


  private final String fightMode;
  private final int initialHealth;
  private final int damage;

  public ImmortalManager(int n, String fightMode) {
    this(n, fightMode,Integer.getInteger("health", 100), Integer.getInteger("damage", 10));
  }

  public ImmortalManager(int n, String fightMode, int initialHealth, int damage) {
    this.fightMode = fightMode;
    this.initialHealth = initialHealth;
    this.damage = damage;
    for (int i=0;i<n;i++) {
      population.add(new Immortal("Immortal-"+i, new AtomicInteger(initialHealth), damage, population, scoreBoard, controller));
    }
    this.initialPopulationSize = n;
  }

  public synchronized void start() {
    if (exec != null) stop();
    exec = Executors.newVirtualThreadPerTaskExecutor();
    for (Immortal im : population) {
      futures.add(exec.submit(im));
    }

    futures.add(exec.submit(()->{
      controller.registerThread();
      try{
        while (!exec.isShutdown()){
          controller.awaitIfPaused();
          if (!controller.paused()) {
            for(Immortal im : population){
              if(im.getHealth() <= 0){
                population.remove(im);
                im.setRunning(false);
              }
            }
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }finally {
        controller.unregisterThread();
      }

    }));

  }

  public void pause() { controller.pause(); }

  public void pauseAndWaitAll() throws InterruptedException {
    controller.pauseAndWaitAll();
  }

  public void resume() { controller.resume(); }
  public void stop() {
    for (Immortal im : population) {
      im.stop();
    }

    if (exec != null) {
      exec.shutdown();
      try {
        if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
          exec.shutdownNow();
        }
      } catch (InterruptedException e) {
        exec.shutdownNow();
        Thread.currentThread().interrupt();
      }
      exec = null;
    }

    futures.clear();
  }

  public synchronized int aliveCount() {
    int c = 0;
    for (Immortal im : population) if (im.isAlive()) c++;
    return c;
  }

  public synchronized long totalHealth() {
    long sum = 0;
    for (Immortal im : population) sum += im.getHealth();
    return sum;
  }

  /**
   * Calcula el invariante esperado de salud total.
   * Invariante: SaludTotal = N * H - (M/2) * F
   * donde N = número de inmortales, H = salud inicial, M = daño, F = número de peleas
   */
  public long expectedTotalHealth() {
    long fights = scoreBoard.totalFights();
    long initialTotal = (long) initialPopulationSize * initialHealth;
    long healthLoss = (damage / 2) * fights;
    return initialTotal - healthLoss;
  }
  public int getInitialPopulationSize() { return initialPopulationSize; }

  public int getInitialHealth() { return initialHealth; }
  public int getDamage() { return damage; }

  public List<Immortal> populationSnapshot() {
    return Collections.unmodifiableList(new ArrayList<>(population));
  }

  public ScoreBoard scoreBoard() { return scoreBoard; }
  public PauseController controller() { return controller; }

  @Override public void close() { stop(); }
}

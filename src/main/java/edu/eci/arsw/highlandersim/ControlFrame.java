package edu.eci.arsw.highlandersim;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import edu.eci.arsw.immortals.Immortal;
import edu.eci.arsw.immortals.ImmortalManager;

public final class ControlFrame extends JFrame {

  private ImmortalManager manager;
  private final JTextArea output = new JTextArea(14, 40);
  private final JButton startBtn = new JButton("Start");
  private final JButton pauseAndCheckBtn = new JButton("Pause & Check");
  private final JButton resumeBtn = new JButton("Resume");
  private final JButton stopBtn = new JButton("Stop");

  private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 5000, 1));
  private final JSpinner healthSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));
  private final JSpinner damageSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
  private final JComboBox<String> fightMode = new JComboBox<>(new String[]{"ordered", "naive"});

  public ControlFrame(int count, String fight) {
    setTitle("Highlander Simulator — ARSW");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout(8,8));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Count:"));
    countSpinner.setValue(count);
    top.add(countSpinner);
    top.add(new JLabel("Health:"));
    top.add(healthSpinner);
    top.add(new JLabel("Damage:"));
    top.add(damageSpinner);
    top.add(new JLabel("Fight:"));
    fightMode.setSelectedItem(fight);
    top.add(fightMode);
    add(top, BorderLayout.NORTH);

    output.setEditable(false);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    add(new JScrollPane(output), BorderLayout.CENTER);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottom.add(startBtn);
    bottom.add(pauseAndCheckBtn);
    bottom.add(resumeBtn);
    bottom.add(stopBtn);
    add(bottom, BorderLayout.SOUTH);

    startBtn.addActionListener(this::onStart);
    pauseAndCheckBtn.addActionListener(this::onPauseAndCheck);
    resumeBtn.addActionListener(this::onResume);
    stopBtn.addActionListener(this::onStop);

    pack();
    setLocationByPlatform(true);
    setVisible(true);
  }

  private void onStart(ActionEvent e) {
    safeStop();
    int n = (Integer) countSpinner.getValue();
    int health = (Integer) healthSpinner.getValue();
    int damage = (Integer) damageSpinner.getValue();
    String fight = (String) fightMode.getSelectedItem();
    manager = new ImmortalManager(n, fight, health, damage);
    manager.start();
    output.setText("Simulation started with %d immortals (health=%d, damage=%d, fight=%s)%n"
      .formatted(n, health, damage, fight));
  }

  private void onPauseAndCheck(ActionEvent e) {
    if (manager == null) return;
    try {
      // Pausar y esperar a que TODOS los hilos estén efectivamente pausados
      manager.pauseAndWaitAll();
      
      // Ahora es seguro leer el estado sin condiciones de carrera
      List<Immortal> pop = manager.populationSnapshot();
      long actualHealth = 0;
      StringBuilder sb = new StringBuilder();
      
      sb.append("=== ESTADO DE LOS INMORTALES ===\n\n");
      for (Immortal im : pop) {
        int h = im.getHealth();
        actualHealth += h;
        sb.append(String.format("%-14s : %5d%n", im.name(), h));
      }
      
      sb.append("\n================================\n");
      long fights = manager.scoreBoard().totalFights();
      long expectedHealth = manager.expectedTotalHealth();
      sb.append(String.format("Peleas totales: %d%n", fights));
      sb.append(String.format("Salud actual:   %d%n", actualHealth));
      sb.append(String.format("Salud esperada: %d%n", expectedHealth));
      
      long diff = Math.abs(actualHealth - expectedHealth);
      boolean invariantHolds = (diff == 0);
      
      sb.append("\n--- INVARIANTE ---\n");
      sb.append(String.format("Fórmula: N*H - (M/2)*F%n"));
      sb.append(String.format("= %d*%d - (%d/2)*%d%n", 
        manager.getInitialPopulationSize(), manager.getInitialHealth(), manager.getDamage(), fights));
      sb.append(String.format("= %d%n", expectedHealth));
      sb.append("\n");
      
      if (invariantHolds) {
        sb.append(" INVARIANTE SE CUMPLE\n");
        sb.append(" Todos los hilos fueron pausados correctamente\n");
        sb.append(" No hay condiciones de carrera\n");
      } else {
        sb.append("INVARIANTE NO SE CUMPLE\n");
        sb.append(String.format("  Diferencia: %d%n", diff));
        sb.append("  Posible condición de carrera\n");
      }
      
      output.setText(sb.toString());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      output.setText("Error: Interrupción durante la pausa\n");
    }
  }

  private void onResume(ActionEvent e) {
    if (manager == null) return;
    manager.resume();
  }

  private void onStop(ActionEvent e) { safeStop(); }

  private void safeStop() {
    if (manager != null) {
      manager.stop();
      manager = null;
    }
  }

  public static void main(String[] args) {
    int count = Integer.getInteger("count", 8);
    String fight = System.getProperty("fight", "ordered");
    SwingUtilities.invokeLater(() -> new ControlFrame(count, fight));
  }
}

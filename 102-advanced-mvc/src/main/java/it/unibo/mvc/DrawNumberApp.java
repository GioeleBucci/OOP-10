package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {

  private final DrawNumber model;
  private final List<DrawNumberView> views;

  /**
   * @param configFile the configuration file
   * @param views      the views to attach
   * @throws FileNotFoundException if config file doesn't exist
   */
  public DrawNumberApp(final File configFile, final DrawNumberView... views) throws FileNotFoundException {

    this.views = Arrays.asList(Arrays.copyOf(views, views.length));
    for (final DrawNumberView view : views) {
      view.setObserver(this);
      view.start();
    }

    if (!configFile.exists()) {
      throw new FileNotFoundException("Config file does not exist");
    }
    final Configuration.Builder configBuilder = new Configuration.Builder();
    try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
      String line;
      while ((line = br.readLine()) != null) {
        var pair = Arrays.asList(line.split(":"));
        String name = pair.get(0).strip();
        int value = Integer.parseInt(pair.get(1).strip());
        switch (name) {
          case "minimum" -> configBuilder.setMin(value);
          case "maximum" -> configBuilder.setMax(value);
          case "attempts" -> configBuilder.setAttempts(value);
          default -> displayErrorToAllViews("Invalid syntax in config file:\n" + name + " does not exist");
        }
      }
    } catch (IOException | NumberFormatException e) {
      displayErrorToAllViews("Error: " + e.getMessage());
    }
    /*
     * Side-effect proof
     */
    this.model = new DrawNumberImpl(configBuilder.build());
  }

  @Override
  public void newAttempt(final int n) {
    try {
      final DrawResult result = model.attempt(n);
      for (final DrawNumberView view : views) {
        view.result(result);
      }
    } catch (IllegalArgumentException e) {
      for (final DrawNumberView view : views) {
        view.numberIncorrect();
      }
    }
  }

  @Override
  public void resetGame() {
    this.model.reset();
  }

  @Override
  public void quit() {
    /*
     * A bit harsh. A good application should configure the graphics to exit by
     * natural termination when closing is hit. To do things more cleanly, attention
     * should be paid to alive threads, as the application would continue to persist
     * until the last thread terminates.
     */
    System.exit(0);
  }

  /**
   * @param args
   *             ignored
   * @throws FileNotFoundException
   */
  public static void main(final String... args) throws FileNotFoundException {
    new DrawNumberApp(new File("src/main/resources/config.yml"),
        new DrawNumberViewImpl(),
        new DrawNumberViewImpl(),
        new PrintStreamView(System.out),
        new PrintStreamView("output.log"));
  }

  private void displayErrorToAllViews(String errMsg) {
    for (final DrawNumberView view : views) {
      view.displayError(errMsg);
    }
  }

}

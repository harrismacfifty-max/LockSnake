package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.Direction;
import de.hsbi.lockgame.model.Level;
import de.hsbi.lockgame.model.Snake;
import de.hsbi.lockgame.observer.GameObservable;
import de.hsbi.lockgame.observer.GameObserver;
import de.hsbi.lockgame.ui.GamePanel;
import java.util.ArrayList;
import java.util.List;

public final class GameEngine implements GameObserver<Direction>, GameObservable<GameState> {
  private GameState state;
  private final List<GameObserver<GameState>> observers = new ArrayList<>();

  public GameEngine(Level level) {
    this.state =
        new GameState(
            level,
            new Snake(List.of(level.snakeStart())),
            level.pins(),
            GameState.Status.RUNNING,
            Direction.NONE);
  }

  public GameState state() {
    return state;
  }

  @Override
  public void addObserver(GameObserver<GameState> observer) {
    if (observer != null) {
      observers.add(observer);
    }
  }

  @Override
  public void removeObserver(GameObserver<GameState> observer) {
    observers.remove(observer);
  }

  public void setGamePanel(GamePanel panel) {
    addObserver(panel);
  }

  @Override
  public void update(Direction d) {
    if (d == null || !state.status().isRunning()) {
      return;
    }

    state = new GameState(state.level(), state.snake(), state.pins(), state.status(), d);
    notifyObservers();
  }

  public void tick() {
    state = state.tick();
    notifyObservers();
  }

  private void notifyObservers() {
    for (GameObserver<GameState> observer : observers) {
      observer.update(state);
    }
  }
}
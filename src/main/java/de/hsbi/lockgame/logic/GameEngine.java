package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.Direction;
import de.hsbi.lockgame.model.Level;
import de.hsbi.lockgame.model.Snake;
import de.hsbi.lockgame.ui.GamePanel;
import java.util.List;

public final class GameEngine {
  private GameState state;
  private GamePanel panel;

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

  public void setGamePanel(GamePanel panel) {
    this.panel = panel;
  }

  public void update(Direction d) {
    if (d == null) {
      return;
    }

    state = new GameState(state.level(), state.snake(), state.pins(), state.status(), d);
    notifyPanel();
  }

  public void tick() {
    state = state.tick();
    notifyPanel();
  }

  private void notifyPanel() {
    if (panel != null) {
      panel.update(state);
    }
  }
}
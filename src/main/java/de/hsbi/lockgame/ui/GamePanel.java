package de.hsbi.lockgame.ui;

import de.hsbi.lockgame.logic.GameEngine;
import de.hsbi.lockgame.logic.GameState;
import de.hsbi.lockgame.model.Direction;
import de.hsbi.lockgame.observer.GameObservable;
import de.hsbi.lockgame.observer.GameObserver;
import de.hsbi.lockgame.settings.GameConstants;
import de.hsbi.lockgame.settings.InputConstants;
import de.hsbi.lockgame.ui.render.GameRenderer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class GamePanel extends JPanel implements GameObserver<GameState>, GameObservable<Direction> {
  private GameState state;
  private final GameRenderer renderer;
  private final List<GameObserver<Direction>> observers = new ArrayList<>();

  public GamePanel(GameState initialState, GameRenderer renderer) {
    this.state = initialState;
    this.renderer = renderer;

    var width = initialState.level().width() * GameConstants.TILE_SIZE;
    var height = initialState.level().height() * GameConstants.TILE_SIZE;

    setPreferredSize(new Dimension(width, height));
    setBackground(Color.BLACK);

    setFocusable(true);
    InputConstants.BINDINGS.forEach(this::setupKeyBindings);
  }

  @Override
  public void update(GameState newState) {
    this.state = newState;
    repaint();
  }

  @Override
  public void addObserver(GameObserver<Direction> observer) {
    if (observer != null) {
      observers.add(observer);
    }
  }

  @Override
  public void removeObserver(GameObserver<Direction> observer) {
    observers.remove(observer);
  }

  public void setGameEngine(GameEngine engine) {
    addObserver(engine);
  }

  private void setupKeyBindings(Direction direction, Iterable<Integer> keyCodes) {
    var inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    var actionMap = getActionMap();

    var actionKey = "move_" + direction.name();

    var swingAction =
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            notifyObservers(direction);
          }
        };

    keyCodes.forEach(keyCode -> inputMap.put(KeyStroke.getKeyStroke(keyCode, 0), actionKey));
    actionMap.put(actionKey, swingAction);
  }

  private void notifyObservers(Direction direction) {
    for (GameObserver<Direction> observer : observers) {
      observer.update(direction);
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    renderer.render((Graphics2D) g, state, GameConstants.TILE_SIZE);
  }
}
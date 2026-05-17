package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.CellType;
import de.hsbi.lockgame.model.Direction;
import de.hsbi.lockgame.model.Level;
import de.hsbi.lockgame.model.Pin;
import de.hsbi.lockgame.model.Position;
import de.hsbi.lockgame.model.Snake;
import java.util.ArrayList;
import java.util.List;

public final class GameState {
  private final Level level;
  private final Snake snake;
  private final List<Pin> pins;
  private final Status status;
  private final Direction pendingDirection;

  public GameState(
      Level level, Snake snake, List<Pin> pins, Status status, Direction pendingDirection) {
    if (level == null) {
      throw new IllegalArgumentException("level must not be null");
    }
    if (snake == null) {
      throw new IllegalArgumentException("snake must not be null");
    }
    if (pins == null) {
      throw new IllegalArgumentException("pins must not be null");
    }
    if (status == null) {
      throw new IllegalArgumentException("status must not be null");
    }
    if (pendingDirection == null) {
      throw new IllegalArgumentException("pendingDirection must not be null");
    }

    this.level = level;
    this.snake = snake;
    this.pins = List.copyOf(pins);
    this.status = status;
    this.pendingDirection = pendingDirection;
  }

  public Level level() {
    return level;
  }

  public Snake snake() {
    return snake;
  }

  public List<Pin> pins() {
    return pins;
  }

  public Status status() {
    return status;
  }

  public Direction pendingDirection() {
    return pendingDirection;
  }

  public GameState tick() {
    if (!status.isRunning() || pendingDirection == Direction.NONE) {
      return this;
    }

    Position nextHead = snake.nextHead(pendingDirection);

    if (!level.isInside(nextHead)) {
      return withStatus(Status.LOST_OUT_OF_BOUNDS);
    }

    if (level.cellAt(nextHead) == CellType.WALL) {
      return withDirection(Direction.NONE);
    }

    if (snakeOccupies(nextHead)) {
      return withStatus(Status.LOST_SELF_COLLISION);
    }

    Pin pin = findPinAt(nextHead);

    if (pin != null) {
      if (pin.state().isSet() || pin.activationDirection() != pendingDirection) {
        return withDirection(Direction.NONE);
      }

      List<Pin> updatedPins = activatePin(pin);
      Status nextStatus = allPinsSet(updatedPins) ? Status.WON : Status.RUNNING;

      return new GameState(level, snake, updatedPins, nextStatus, Direction.NONE);
    }

    return new GameState(level, snake.grow(pendingDirection), pins, status, pendingDirection);
  }

  private GameState withStatus(Status newStatus) {
    return new GameState(level, snake, pins, newStatus, pendingDirection);
  }

  private GameState withDirection(Direction newDirection) {
    return new GameState(level, snake, pins, status, newDirection);
  }

  private Pin findPinAt(Position position) {
    for (Pin pin : pins) {
      if (samePosition(pin.position(), position)) {
        return pin;
      }
    }
    return null;
  }

  private boolean snakeOccupies(Position position) {
    for (Position bodyPart : snake.body()) {
      if (samePosition(bodyPart, position)) {
        return true;
      }
    }
    return false;
  }

  private List<Pin> activatePin(Pin pinToActivate) {
    List<Pin> updatedPins = new ArrayList<>(pins.size());

    for (Pin pin : pins) {
      if (samePosition(pin.position(), pinToActivate.position())) {
        updatedPins.add(pin.withState(Pin.State.HIGH));
      } else {
        updatedPins.add(pin);
      }
    }

    return updatedPins;
  }

  private boolean allPinsSet(List<Pin> pinsToCheck) {
    return pinsToCheck.stream().allMatch(pin -> pin.state().isSet());
  }

  private boolean samePosition(Position first, Position second) {
    return first.x() == second.x() && first.y() == second.y();
  }

  public enum Status {
    RUNNING,
    WON,
    LOST_SELF_COLLISION,
    LOST_OUT_OF_BOUNDS;

    public boolean isRunning() {
      return this == RUNNING;
    }
  }
}
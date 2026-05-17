package de.hsbi.lockgame.observer;

@FunctionalInterface
public interface GameObserver<T> {
  void update(T value);
}
package de.hsbi.lockgame.observer;

public interface GameObservable<T> {
  void addObserver(GameObserver<T> observer);

  void removeObserver(GameObserver<T> observer);
}
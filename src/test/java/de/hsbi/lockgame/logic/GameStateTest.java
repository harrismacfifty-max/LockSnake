package de.hsbi.lockgame.logic;

import static org.junit.jupiter.api.Assertions.*;

import de.hsbi.lockgame.model.CellType;
import de.hsbi.lockgame.model.Direction;
import de.hsbi.lockgame.model.Level;
import de.hsbi.lockgame.model.Pin;
import de.hsbi.lockgame.model.Position;
import de.hsbi.lockgame.model.Snake;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit-Tests für die Spielzustandslogik in {@link GameState}. */
class GameStateTest {

    @Test
    void givenInitialRunningStateWithoutDirection_whenTick_thenStateDoesNotChange() {
        // given
        // Ein Spielzustand wird erzeugt, aber es wurde noch keine Bewegungsrichtung gesetzt.
        // Das entspricht dem Initialzustand direkt nach Spielstart.
        GameState state =
                new GameState(
                        emptyLevel(5, 5, new Position(2, 2)),
                        snakeAt(new Position(2, 2)),
                        List.of(),
                        GameState.Status.RUNNING,
                        Direction.NONE);

        // when
        // Ein Tick wird ausgeführt.
        GameState next = state.tick();

        // then
        // Ohne Richtung darf sich die Schlange nicht bewegen.
        assertSame(state, next);
        assertEquals(GameState.Status.RUNNING, next.status());
        assertPositionEquals(new Position(2, 2), next.snake().head());
    }

    @Test
    void givenRunningStateWithDirectionRight_whenTick_thenSnakeMovesRightAndGrows() {
        // given
        // Die Schlange startet bei (2, 2) und blickt nach rechts.
        GameState state =
                new GameState(
                        emptyLevel(5, 5, new Position(2, 2)),
                        snakeAt(new Position(2, 2)),
                        List.of(),
                        GameState.Status.RUNNING,
                        Direction.RIGHT);

        // when
        // Ein Tick wird ausgeführt.
        GameState next = state.tick();

        // then
        // Der neue Kopf liegt ein Feld rechts bei (3, 2).
        // Da Snake nur grow() besitzt, wächst die Schlange dabei auf Länge 2.
        assertEquals(GameState.Status.RUNNING, next.status());
        assertEquals(Direction.RIGHT, next.pendingDirection());
        assertPositionEquals(new Position(3, 2), next.snake().head());
        assertEquals(2, next.snake().body().size());
    }

    @Test
    void givenRunningStateWithDirectionUp_whenTick_thenSnakeMovesUpAndGrows() {
        // given
        // Die Schlange startet bei (2, 2) und blickt nach oben.
        GameState state =
                new GameState(
                        emptyLevel(5, 5, new Position(2, 2)),
                        snakeAt(new Position(2, 2)),
                        List.of(),
                        GameState.Status.RUNNING,
                        Direction.UP);

        // when
        GameState next = state.tick();

        // then
        // Beim Schritt nach oben sinkt die y-Koordinate von 2 auf 1.
        assertEquals(GameState.Status.RUNNING, next.status());
        assertPositionEquals(new Position(2, 1), next.snake().head());
        assertEquals(2, next.snake().body().size());
    }

    @Test
    void givenSnakeFacingWall_whenTick_thenSnakeIsBlockedAndDirectionBecomesNone() {
        // given
        // Rechts neben der Schlange befindet sich eine Wand.
        CellType[][] cells = emptyCells(5, 5);
        cells[3][2] = CellType.WALL;

        GameState state =
                new GameState(
                        new Level(5, 5, cells, List.of(), new Position(2, 2)),
                        snakeAt(new Position(2, 2)),
                        List.of(),
                        GameState.Status.RUNNING,
                        Direction.RIGHT);

        // when
        GameState next = state.tick();

        // then
        // Die Schlange läuft nicht in die Wand hinein.
        // Stattdessen bleibt sie stehen und die Richtung wird zurückgesetzt.
        assertEquals(GameState.Status.RUNNING, next.status());
        assertEquals(Direction.NONE, next.pendingDirection());
        assertPositionEquals(new Position(2, 2), next.snake().head());
        assertEquals(1, next.snake().body().size());
    }

    @Test
    void givenSnakeFacingOutsideBoard_whenTick_thenGameIsLostOutOfBounds() {
        // given
        // Die Schlange steht am linken Rand und blickt weiter nach links.
        GameState state =
                new GameState(
                        emptyLevel(5, 5, new Position(0, 2)),
                        snakeAt(new Position(0, 2)),
                        List.of(),
                        GameState.Status.RUNNING,
                        Direction.LEFT);

        // when
        GameState next = state.tick();

        // then
        // Der nächste Kopf läge außerhalb des Spielfelds.
        // Das Spiel muss deshalb mit LOST_OUT_OF_BOUNDS enden.
        assertEquals(GameState.Status.LOST_OUT_OF_BOUNDS, next.status());
        assertPositionEquals(new Position(0, 2), next.snake().head());
    }

    @Test
    void givenSnakeFacingOwnBody_whenTick_thenGameIsLostBySelfCollision() {
        // given
        // Die Schlange besteht aus mehreren Körperteilen.
        // Der Kopf steht bei (2, 1), direkt darunter liegt bereits ein Körperteil bei (2, 2).
        Snake snake =
                new Snake(
                        List.of(
                                new Position(2, 1),
                                new Position(2, 2),
                                new Position(1, 2),
                                new Position(1, 1)));

        GameState state =
                new GameState(
                        emptyLevel(5, 5, new Position(2, 1)),
                        snake,
                        List.of(),
                        GameState.Status.RUNNING,
                        Direction.DOWN);

        // when
        GameState next = state.tick();

        // then
        // Der nächste Kopf würde auf ein eigenes Körperteil laufen.
        // Das Spiel muss deshalb mit LOST_SELF_COLLISION enden.
        assertEquals(GameState.Status.LOST_SELF_COLLISION, next.status());
    }

    @Test
    void givenSnakeFacingPinFromWrongDirection_whenTick_thenPinBlocksSnake() {
        // given
        // Die Schlange steht links vom Pin und bewegt sich nach rechts.
        // Der Pin erwartet aber Aktivierung aus Richtung LEFT.
        // Dadurch ist die tatsächliche Bewegungsrichtung RIGHT falsch.
        Pin pin = new Pin(new Position(2, 1), Pin.State.LOW, Direction.LEFT);

        GameState state =
                new GameState(
                        levelWithPins(5, 5, new Position(1, 1), List.of(pin)),
                        snakeAt(new Position(1, 1)),
                        List.of(pin),
                        GameState.Status.RUNNING,
                        Direction.RIGHT);

        // when
        GameState next = state.tick();

        // then
        // Der Pin wird nicht gesetzt.
        // Die Schlange bleibt stehen und die Richtung wird auf NONE gesetzt.
        assertEquals(GameState.Status.RUNNING, next.status());
        assertEquals(Direction.NONE, next.pendingDirection());
        assertPositionEquals(new Position(1, 1), next.snake().head());
        assertFalse(next.pins().getFirst().state().isSet());
    }

    @Test
    void givenSnakeFacingAlreadySetPin_whenTick_thenPinBlocksSnake() {
        // given
        // Der Pin ist bereits HIGH.
        // Bereits gesetzte Pins sollen nicht erneut betreten oder aktiviert werden.
        Pin pin = new Pin(new Position(2, 1), Pin.State.HIGH, Direction.RIGHT);

        GameState state =
                new GameState(
                        levelWithPins(5, 5, new Position(1, 1), List.of(pin)),
                        snakeAt(new Position(1, 1)),
                        List.of(pin),
                        GameState.Status.RUNNING,
                        Direction.RIGHT);

        // when
        GameState next = state.tick();

        // then
        // Die Schlange wird blockiert.
        // Der Pin bleibt HIGH.
        assertEquals(GameState.Status.RUNNING, next.status());
        assertEquals(Direction.NONE, next.pendingDirection());
        assertPositionEquals(new Position(1, 1), next.snake().head());
        assertTrue(next.pins().getFirst().state().isSet());
    }

    @Test
    void givenSnakeFacingPinFromCorrectDirection_whenTick_thenPinIsActivatedAndSnakeStaysBeforePin() {
        // given
        // Die Schlange steht links vom Pin und bewegt sich nach rechts.
        // Der Pin erwartet genau Direction.RIGHT und ist noch LOW.
        // Zusätzlich gibt es einen zweiten LOW-Pin, damit das Spiel nach dem ersten Pin noch nicht gewonnen ist.
        Pin firstPin = new Pin(new Position(2, 1), Pin.State.LOW, Direction.RIGHT);
        Pin secondPin = new Pin(new Position(4, 4), Pin.State.LOW, Direction.UP);

        GameState state =
                new GameState(
                        levelWithPins(5, 5, new Position(1, 1), List.of(firstPin, secondPin)),
                        snakeAt(new Position(1, 1)),
                        List.of(firstPin, secondPin),
                        GameState.Status.RUNNING,
                        Direction.RIGHT);

        // when
        GameState next = state.tick();

        // then
        // Der erste Pin wird aktiviert.
        // Die Schlange bewegt sich dabei nicht auf das Pin-Feld.
        // Das Spiel läuft weiter, weil noch nicht alle Pins gesetzt sind.
        assertEquals(GameState.Status.RUNNING, next.status());
        assertEquals(Direction.NONE, next.pendingDirection());
        assertPositionEquals(new Position(1, 1), next.snake().head());
        assertTrue(pinAt(next.pins(), new Position(2, 1)).state().isSet());
        assertFalse(pinAt(next.pins(), new Position(4, 4)).state().isSet());
    }

    @Test
    void givenSnakeActivatesLastUnsetPin_whenTick_thenGameIsWon() {
        // given
        // Es gibt genau einen Pin.
        // Wenn dieser aktiviert wird, sind danach alle Pins gesetzt.
        Pin pin = new Pin(new Position(2, 1), Pin.State.LOW, Direction.RIGHT);

        GameState state =
                new GameState(
                        levelWithPins(5, 5, new Position(1, 1), List.of(pin)),
                        snakeAt(new Position(1, 1)),
                        List.of(pin),
                        GameState.Status.RUNNING,
                        Direction.RIGHT);

        // when
        GameState next = state.tick();

        // then
        // Der Pin wird HIGH und das Spiel ist gewonnen.
        assertEquals(GameState.Status.WON, next.status());
        assertTrue(next.pins().getFirst().state().isSet());
        assertPositionEquals(new Position(1, 1), next.snake().head());
    }

    @Test
    void givenWonState_whenTick_thenStateDoesNotChange() {
        // given
        // Das Spiel ist bereits gewonnen.
        // Auch wenn eine Richtung gesetzt ist, soll kein weiterer Spielschritt stattfinden.
        GameState state =
                new GameState(
                        emptyLevel(5, 5, new Position(2, 2)),
                        snakeAt(new Position(2, 2)),
                        List.of(),
                        GameState.Status.WON,
                        Direction.RIGHT);

        // when
        GameState next = state.tick();

        // then
        // Beendete Spiele werden nicht weiter verändert.
        assertSame(state, next);
        assertEquals(GameState.Status.WON, next.status());
        assertPositionEquals(new Position(2, 2), next.snake().head());
    }

    @Test
    void givenLostState_whenTick_thenStateDoesNotChange() {
        // given
        // Das Spiel ist bereits verloren.
        GameState state =
                new GameState(
                        emptyLevel(5, 5, new Position(2, 2)),
                        snakeAt(new Position(2, 2)),
                        List.of(),
                        GameState.Status.LOST_SELF_COLLISION,
                        Direction.RIGHT);

        // when
        GameState next = state.tick();

        // then
        // Auch verlorene Spiele dürfen nicht weiterlaufen.
        assertSame(state, next);
        assertEquals(GameState.Status.LOST_SELF_COLLISION, next.status());
    }

    private static Level emptyLevel(int width, int height, Position snakeStart) {
        return new Level(width, height, emptyCells(width, height), List.of(), snakeStart);
    }

    private static Level levelWithPins(int width, int height, Position snakeStart, List<Pin> pins) {
        CellType[][] cells = emptyCells(width, height);

        for (Pin pin : pins) {
            Position position = pin.position();
            cells[position.x()][position.y()] = CellType.PIN_SLOT;
        }

        return new Level(width, height, cells, pins, snakeStart);
    }

    private static CellType[][] emptyCells(int width, int height) {
        CellType[][] cells = new CellType[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells[x][y] = CellType.EMPTY;
            }
        }

        return cells;
    }

    private static Snake snakeAt(Position head) {
        return new Snake(List.of(head));
    }

    private static Pin pinAt(List<Pin> pins, Position position) {
        return pins.stream()
                .filter(pin -> hasSameCoordinates(pin.position(), position))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected pin at position"));
    }

    private static void assertPositionEquals(Position expected, Position actual) {
        assertEquals(expected.x(), actual.x(), "x coordinate");
        assertEquals(expected.y(), actual.y(), "y coordinate");
    }

    private static boolean hasSameCoordinates(Position first, Position second) {
        return first.x() == second.x() && first.y() == second.y();
    }
}
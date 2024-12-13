package ru.itis.gui.components;

import javafx.animation.FadeTransition;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.itis.core.Movement;
import ru.itis.core.Stone;

import static ru.itis.core.Goban.GRID_SIZE;

@EqualsAndHashCode(callSuper = true)
@Data
public class GobanPane extends GridPane {
    private Stone[][] stones = new Stone[GRID_SIZE][GRID_SIZE];

    public void addStone(Stone stone) {
        stones[stone.getX()][stone.getY()] = stone;
        add(stone, stone.getY(), stone.getX());
    }

    public Stone getStone(int x, int y) {
        return stones[x][y];
    }

    public void setCapturedStone(Movement movement) {
        Stone stone = stones[movement.getX()][movement.getY()];
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), stone);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.play();
    }
}

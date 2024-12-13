package ru.itis.core;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stone extends Circle {

    private int x;
    private int y;
    private Color color = Color.TRANSPARENT;
    private boolean isBlocked = false;
    private int player = 0;
    private Chain chain = new Chain();

    public static final int RADIUS = 57;

    public Stone(int x, int y) {
        this.x = x;
        this.y = y;
        setRadius(RADIUS);
        setFill(color);
        chain.addStone(this);
    }

    public void setColor(Color color) {
        this.color = color;
        setFill(color);
    }

    public void setBlocked(boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stone stone)) return false;
        return x == stone.x && y == stone.y;
    }

    @Override
    public String toString() {
        return "Stone{" +
                "(" + x + ", " + y + ")" +
                ", player=" + player +
                '}';
    }
}

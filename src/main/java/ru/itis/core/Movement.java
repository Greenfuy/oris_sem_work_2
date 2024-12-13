package ru.itis.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movement {
    private int x;
    private int y;

    public Movement(Stone stone) {
        this(stone.getX(), stone.getY());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movement movement)) return false;
        return x == movement.x && y == movement.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

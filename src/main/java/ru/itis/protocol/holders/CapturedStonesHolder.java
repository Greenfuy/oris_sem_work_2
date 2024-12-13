package ru.itis.protocol.holders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.itis.core.Movement;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapturedStonesHolder {
    private Movement[] capturedStones;
}

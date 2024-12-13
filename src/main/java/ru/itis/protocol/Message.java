package ru.itis.protocol;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private byte type;
    private String body;

    @Override
    public String toString() {
        return type + " " + body;
    }
}

package ru.itis.protocol.exceptions;

public class IllegalMessageTypeException extends RuntimeException {
    public IllegalMessageTypeException(String message) {
        super(message);
    }

}

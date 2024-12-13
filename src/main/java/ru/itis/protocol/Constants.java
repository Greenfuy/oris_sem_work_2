package ru.itis.protocol;

public class Constants {

    public static final String LOCALHOST = "localhost";
    public static final int PORT = 8080;

    //CLIENT
    public static final byte START_GAME = 0;
    public static final byte MOVE = 1; //with data (x,y)
    public static final byte PASS = 2;
    public static final byte RESIGN = 3;

    //SERVER
    public static final byte SEND_GAME_STARTED = -1; //with data (number)
    public static final byte ILLEGAL_MESSAGE = -2;
    public static final byte SEND_MOVE = -3; //with data (x,y)
    public static final byte SEND_PASS = -4;
    public static final byte SEND_RESIGN = -5;
    public static final byte PLAYER_WINNER = -6; //with data (number,score)
    public static final byte SEND_BLOCK = -7; //with data (json)
    public static final byte SEND_TURN = -8;
    public static final byte SEND_DISCONNECTED = -9;
    public static final byte SEND_JOINED = -10;

}

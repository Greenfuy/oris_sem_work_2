package ru.itis.client;

import lombok.Data;
import lombok.SneakyThrows;
import ru.itis.protocol.Message;
import ru.itis.protocol.exceptions.IllegalMessageTypeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static ru.itis.protocol.Constants.*;

@Data
public class PlayerClient {

    private final Socket socket;
    private final PrintWriter printWriter;
    private final BufferedReader bufferedReader;
    private boolean connected;

    @SneakyThrows
    public PlayerClient(Socket socket) {
        this.socket = socket;
        this.printWriter = new PrintWriter(socket.getOutputStream(),true);
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
    }

    public void sendMessage(byte type, String body) {
        if (type < SEND_JOINED || type > SEND_GAME_STARTED) {
            connected = false;
            throw new IllegalMessageTypeException("Illegal message type of " + type);
        } else {
            try {
                printWriter.println(new Message(type, body));
                System.out.println("send: " + type + " " + body);
            } catch (Exception e) {
                connected = false;
            }
        }
    }


    public String receiveMessage() {
       try {
           String message = bufferedReader.readLine();
           System.out.println("receive: " + message);
           return message;
       } catch (IOException e) {
           connected = false;
           return null;
       }
    }

    @SneakyThrows
    public boolean isConnected() {
        return connected && socket.isConnected() && !socket.isClosed();
    }
}

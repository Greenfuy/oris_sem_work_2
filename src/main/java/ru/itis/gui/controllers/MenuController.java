package ru.itis.gui.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.itis.protocol.Message;
import server.GoGameServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

import static ru.itis.protocol.Constants.*;

public class MenuController {

    @FXML
    private Button startGameButton;
    @FXML
    private Text failText;
    private Socket socket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;

    private static final String FAILED_TO_CONNECT = "Failed to connect";
    private static final String SOMETHING_WENT_WRONG = "Something went wrong";


    @FXML
    void onStartGameBtnClicked(ActionEvent event) {
        hideFailText();
        try {
            socket = new Socket(LOCALHOST, PORT);
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            sendMessage(START_GAME);
            new Thread(new OnStartGameTask(event)).start();
            Stage currentStage = (Stage) startGameButton.getScene().getWindow();
            currentStage.setOnCloseRequest(windowEvent -> {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                socket = null;
            });
        } catch (IOException e) {
            showFailText(FAILED_TO_CONNECT);
        }
    }

    private void showFailText(String message) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), failText);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        failText.setText(message);
        failText.setVisible(true);

        fadeIn.play();
    }

    private void hideFailText() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), failText);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        failText.setText("");
        failText.setVisible(false);

        fadeOut.play();
    }

    private void sendMessage(byte type) {
        System.out.println("send: " + type);
        printWriter.println(type + " ");
    }

    private Message receiveMessage() throws IOException {
        if (socket != null) {
            String message = bufferedReader.readLine();
            System.out.println("receive: " + message);
            String[] split = message.split(" ");
            if (GoGameServer.isNumeric(split[0])) {
                byte type = Byte.parseByte(split[0]);
                String body = split.length > 1 ? split[1] : "";
                return new Message(type, body);
            }
        }
        return null;
    }

    private void onStartGame() throws IOException {
        Message message = receiveMessage();
        while (message == null) message = receiveMessage();
        if (message.getType() == SEND_JOINED) {
            Platform.runLater(() -> {
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(
                            Objects.requireNonNull(getClass().getResource("/game-view.fxml"))
                    );

                    Parent root = fxmlLoader.load();
                    GameController gameController = fxmlLoader.getController();
                    gameController.setSocket(socket);

                    Scene scene = startGameButton.getScene();
                    scene.setRoot(root);
                } catch (IOException e) {
                    showFailText(SOMETHING_WENT_WRONG);
                    System.out.println(e.getMessage());
                }
            });
        }
    }

    private class OnStartGameTask extends Task<Void> {
        private final ActionEvent event;

        public OnStartGameTask(ActionEvent event) {
            this.event = event;
        }

        @Override
        protected Void call() throws Exception {
            onStartGame();
            return null;
        }
    }
}

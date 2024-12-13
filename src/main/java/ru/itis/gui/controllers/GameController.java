package ru.itis.gui.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import lombok.SneakyThrows;
import ru.itis.gui.components.GobanPane;
import ru.itis.core.Movement;
import ru.itis.core.Stone;
import ru.itis.protocol.Message;
import ru.itis.protocol.holders.CapturedStonesHolder;
import server.GoGameServer;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

import static javafx.geometry.HPos.CENTER;
import static ru.itis.core.Goban.*;
import static ru.itis.protocol.Constants.*;

public class GameController {

    private Socket socket;
    @FXML
    private VBox rootVBox;
    @FXML
    private GobanPane gobanPane;
    @FXML
    private HBox buttonsHBox;
    @FXML
    private Button passButton;
    @FXML
    private Button resignButton;
    @FXML
    private Text winText;
    @FXML
    private Canvas gridCanvas;
    private Stone prevStone;
    private int player;
    private boolean canMove = false;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ENEMY_PASSED = "Enemy passed. Your turn";
    private static final String ENEMY_RESIGNED = "Enemy resigned. You won";
    private static final String YOU_RESIGNED = "You resigned. You lost";
    private static final String YOU_WON = "You won with score: ";
    private static final String YOU_LOST = "You lost with score: ";
    private static final String ENEMY_DISCONNECTED = "Enemy disconnected";
    private static final String LOST_CONNECTION = "Lost connection";
    private static final String WAITING_FOR_OPPONENT = "Waiting for opponent";
    public static final int STONE_STROKE_WIDTH = 7;
    public static final int CANVAS_GRID_SIZE = 4;

    @FXML
    void initialize() {
        initGobanPane();
        VBox.setMargin(buttonsHBox, new Insets(0, 0, 20, 0));
        drawGrid(gridCanvas.getGraphicsContext2D());
        showWinText(WAITING_FOR_OPPONENT);
        setButtonsBlocked();
    }

    @FXML
    void onPassBtnClicked(ActionEvent event) {
        if (canMove) {
            sendMessage(PASS, "");
            canMove = false;
            setButtonsBlocked();
            new Thread(new ShowOpponentMoveTask()).start();
            socket = null;
        }
    }

    @FXML
    void onResignBtnClicked(ActionEvent event) {
        if (canMove) {
            sendMessage(RESIGN, "");
            canMove = false;
            showWinText(YOU_RESIGNED);
            winText.setOnMouseClicked(e -> onWinTextWhenGameEndedClicked());
            setButtonsBlocked();
            new Thread(new ShowOpponentMoveTask()).start();
        }
    }

    private void setButtonsBlocked() {
        gobanPane.setDisable(!canMove);
        passButton.setDisable(!canMove);
        resignButton.setDisable(!canMove);
    }

    public void setSocket(Socket socket) throws IOException {
        if (socket == null || !socket.isConnected()) {
            endGameDisconnected(LOST_CONNECTION);
        } else {
            this.socket = socket;
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(new GetPlayerFromServerTask()).start();
        }
    }

    public void initGobanPane() {
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                Stone stone = new Stone(x, y);

                stone.setOnMouseClicked(e -> new Thread(new OnStoneClickedTask(stone)).start());

                GridPane.setHalignment(stone, CENTER);
                gobanPane.addStone(stone);
            }
        }

        gobanPane.prefWidthProperty().bind(rootVBox.heightProperty());
        gobanPane.prefHeightProperty().bind(rootVBox.widthProperty());
    }

    private void highlightStone(Stone stone, Color color) {
        if (prevStone != null) {
            prevStone.setStroke(Color.TRANSPARENT);
            prevStone.setStrokeWidth(0);
        }
        prevStone = stone;
        prevStone.setFill(color);
        prevStone.setStroke(Color.LIGHTGREY);
        prevStone.setStrokeWidth(STONE_STROKE_WIDTH);
    }

    private void getPlayerFromServer() {
        Message message = receiveMessage();
        while (message == null) {
            message = receiveMessage();
        }

        switch (message.getType()) {
            case SEND_GAME_STARTED -> {
                player = Integer.parseInt(message.getBody());
                Platform.runLater(() -> {
                    if (player == PLAYER_1) {
                        canMove = true;
                        setButtonsBlocked();
                    } else {
                        new Thread(new ShowOpponentMoveTask()).start();
                    }
                    hideWinText();
                });
            }
            case SEND_DISCONNECTED -> endGameDisconnected(ENEMY_DISCONNECTED);
        }
    }

    private void sendMessage(byte type, String body) {
        System.out.println("send: " + type + " " + body);
        printWriter.println(type + " " + body);
    }

    private Message receiveMessage() {
        try {
            if (bufferedReader.ready()) {
                String message = bufferedReader.readLine();
                System.out.println("receive: " + message);
                String[] split = message.split(" ");
                if (GoGameServer.isNumeric(split[0])) {
                    byte type = Byte.parseByte(split[0]);
                    String body = split.length > 1 ? split[1] : "";
                    return new Message(Byte.parseByte(split[0]), body);
                }
                return null;
            }
        } catch (IOException e) {
            endGameDisconnected(LOST_CONNECTION);
        }
        return null;
    }

    @SneakyThrows
    private void onStoneClicked(Stone stone) {
        if (canMove && !stone.isBlocked()) {
            sendMessage(MOVE, stone.getX() + "," + stone.getY());

            if (makeTurn(stone)) {
                new Thread(new ShowOpponentMoveTask()).start();
            }
        }
    }

    private boolean makeTurn(Stone stone) throws IOException {
        Message message = receiveMessage();
        while (message == null) message = receiveMessage();

        switch (message.getType()) {
            case SEND_BLOCK -> {
                canMove = true;
                Platform.runLater(this::setButtonsBlocked);
                stone.setBlocked(true);
                return false;
            }
            case SEND_TURN -> {
                ObjectMapper objectMapper = new ObjectMapper();
                CapturedStonesHolder capturedStonesHolder =
                        message.getBody().isEmpty() ?
                                new CapturedStonesHolder() :
                                objectMapper.readValue(message.getBody(), CapturedStonesHolder.class);

                if (capturedStonesHolder.getCapturedStones() != null) {
                    Platform.runLater(() -> setStonesCaptured(capturedStonesHolder.getCapturedStones()));
                }
                canMove = false;
                Platform.runLater(() -> {
                    setButtonsBlocked();
                    highlightStone(stone, player == PLAYER_1 ? PlAYER_1_COLOR : PlAYER_2_COLOR);
                });
                return true;
            }
        }
        return false;
    }

    private void showOpponentMove() throws IOException {
        Message message = receiveMessage();
        while (message == null) message = receiveMessage();

        switch (message.getType()) {
            case SEND_MOVE -> {
                Movement[] capturedStones = objectMapper.readValue(
                        message.getBody(),
                        CapturedStonesHolder.class
                ).getCapturedStones();

                canMove = true;
                Platform.runLater(() -> {
                    setButtonsBlocked();
                    setOpponentMove(capturedStones);
                });
            }
            case SEND_PASS -> {
                canMove = true;
                Platform.runLater(() -> {
                    setButtonsBlocked();
                    showWinText(ENEMY_PASSED);
                    winText.setOnMouseClicked(e -> onWinTextClicked());
                });
            }
            case SEND_RESIGN -> {
                canMove = false;
                Platform.runLater(() -> {
                    setButtonsBlocked();
                    showWinText(ENEMY_RESIGNED);
                });
                socket = null;
            }
            case PLAYER_WINNER -> endGame(message);
            case SEND_DISCONNECTED -> endGameDisconnected(ENEMY_DISCONNECTED);
        }

    }

    @SneakyThrows
    private void endGame(Message message) {
        canMove = false;
        String[] split = message.getBody().split(",");
        int winner = Integer.parseInt(split[0]);
        int score = Integer.parseInt(split[1]);
        Platform.runLater(() -> {
            setButtonsBlocked();
            showWinText(winner == player ? YOU_WON + score : YOU_LOST + score);
            winText.setOnMouseClicked(e -> onWinTextWhenGameEndedClicked());
        });
        socket.close();
        socket = null;
    }

    @SneakyThrows
    private void endGameDisconnected(String message) {
        canMove = false;
        Platform.runLater(() -> {
            setButtonsBlocked();
            showWinText(message);
            winText.setOnMouseClicked(e -> onWinTextWhenGameEndedClicked());
        });
        socket.close();
        socket = null;
    }

    private void setOpponentMove(Movement[] capturedStones) {
        Stone stone = gobanPane.getStone(capturedStones[0].getX(), capturedStones[0].getY());
        highlightStone(stone, player == PLAYER_1 ? PlAYER_2_COLOR : PlAYER_1_COLOR);
        if (capturedStones.length > 1) {
            for (int i = 1; i < capturedStones.length; i++) {
                gobanPane.setCapturedStone(capturedStones[i]);
            }
        }
    }

    private void setStonesCaptured(Movement[] capturedStones) {
        for (Movement movement : capturedStones) {
            gobanPane.setCapturedStone(movement);
        }
    }

    private void showWinText(String text) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), winText);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        winText.setVisible(true);
        winText.setText(text);

        fadeIn.play();
    }

    private void hideWinText() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), winText);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        winText.setVisible(false);
        winText.setOnMouseClicked(null);
        winText.setText("");

        fadeOut.play();
    }

    private void onWinTextClicked() {
        hideWinText();
    }

    @SneakyThrows
    private void onWinTextWhenGameEndedClicked() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/menu-view.fxml"))
        );
        Parent root = fxmlLoader.load();
        Scene scene = passButton.getScene();
        scene.setRoot(root);
    }

    private void drawGrid(GraphicsContext graphicsContext) {
        double canvasSize = graphicsContext.getCanvas().getHeight();
        double cellSize = canvasSize / CANVAS_GRID_SIZE;

        for (int i = 0; i <= CANVAS_GRID_SIZE; i++) {
            double x = i * cellSize;
            if (i == 0 || i == CANVAS_GRID_SIZE) {
                graphicsContext.setLineWidth(2 * STONE_STROKE_WIDTH);
            } else {
                graphicsContext.setLineWidth(STONE_STROKE_WIDTH);
            }
            graphicsContext.strokeLine(x, 0, x, canvasSize);
        }

        for (int i = 0; i <= CANVAS_GRID_SIZE; i++) {
            double y = i * cellSize;
            if (i == 0 || i == CANVAS_GRID_SIZE) {
                graphicsContext.setLineWidth(2 * STONE_STROKE_WIDTH);
            } else {
                graphicsContext.setLineWidth(STONE_STROKE_WIDTH);
            }
            graphicsContext.strokeLine(0, y, canvasSize, y);
        }
    }

    private class GetPlayerFromServerTask extends Task<Void> {
        @SneakyThrows
        @Override
        public Void call() {
            getPlayerFromServer();
            return null;
        }
    }

    private class ShowOpponentMoveTask extends Task<Void> {
        @Override
        public Void call() throws Exception {
            showOpponentMove();
            return null;
        }
    }

    private class OnStoneClickedTask extends Task<Void> {
        private final Stone stone;

        public OnStoneClickedTask(Stone stone) {
            this.stone = stone;
        }

        @Override
        public Void call() {
            onStoneClicked(stone);
            return null;
        }
    }
}

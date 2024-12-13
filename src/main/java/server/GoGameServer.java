package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import ru.itis.client.PlayerClient;
import ru.itis.core.Goban;
import ru.itis.core.Movement;
import ru.itis.protocol.Message;
import ru.itis.protocol.holders.CapturedStonesHolder;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;
import static ru.itis.core.Goban.PLAYER_1;
import static ru.itis.core.Goban.PLAYER_2;
import static ru.itis.protocol.Constants.*;

public class GoGameServer {

    private static Goban goban;
    private static int passCount = 0;
    private static int player = 1;
    private static boolean isGameInProgress = true;

    private static final Map<Integer, PlayerClient> clients = new HashMap<>();

    @SneakyThrows
    public static void main(String[] args) {
        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            goban = new Goban();
            isGameInProgress = true;

            Socket player1Socket = serverSocket.accept();
            clients.put(PLAYER_1, new PlayerClient(player1Socket));
            clients.get(PLAYER_1).sendMessage(SEND_JOINED, "");

            if (clients.get(PLAYER_1).isConnected()) {
                Socket player2Socket = serverSocket.accept();
                clients.put(PLAYER_2, new PlayerClient(player2Socket));
                clients.get(PLAYER_2).sendMessage(SEND_JOINED, "");
            } else {
                continue;
            }

            if (clients.get(PLAYER_1).isConnected() && clients.get(PLAYER_2).isConnected()) {
                clients.get(PLAYER_1).sendMessage(SEND_GAME_STARTED, PLAYER_1 + "");
                sleep(100);
                clients.get(PLAYER_2).sendMessage(SEND_GAME_STARTED, PLAYER_2 + "");
            } else {
                endGame(SEND_DISCONNECTED);
                continue;
            }

            while (isGameInProgress) {
                if (!clients.get(PLAYER_1).isConnected() || !clients.get(PLAYER_2).isConnected()) {
                    endGame(SEND_DISCONNECTED);
                }
                if (passCount == 2) {
                    endGame(PLAYER_WINNER);
                }
                String messageGet = clients.get(player).receiveMessage();
                if (messageGet == null) continue;
                Message message = parseMessage(messageGet);
                if (message == null) {
                    clients.get(player).sendMessage(ILLEGAL_MESSAGE, "");
                } else {
                    byte type = message.getType();
                    switch (type) {
                        case MOVE -> {
                            Movement movement = getMovementFromBody(message.getBody());
                            if (goban.checkTurnAccess(movement)) {
                                List<Movement> capturedStones = goban.makeTurn(movement);
                                String json =  capturedStones.isEmpty() ? "" : getCapturedStonesJson(capturedStones);
                                clients.get(player)
                                        .sendMessage(SEND_TURN, json);
                                capturedStones.add(0, movement);
                                clients.get(getEnemyPlayer())
                                        .sendMessage(SEND_MOVE, getCapturedStonesJson(capturedStones));
                                changePlayerTurn();
                            } else {
                                clients.get(player).sendMessage(SEND_BLOCK, "");
                            }
                        }
                        case PASS -> {
                            passCount++;
                            if (passCount < 2) {
                                clients.get(getEnemyPlayer()).sendMessage(SEND_PASS, "");
                                changePlayerTurn();
                            }
                        }
                        case RESIGN -> endGame(RESIGN);
                    }
                }
            }
        }
    }

    @SneakyThrows
    private static void endGame(byte type) {
        Integer score = goban.getScore();
        int winner = goban.getWinnerPlayer();

        switch (type) {
            case SEND_RESIGN -> clients.get(getEnemyPlayer()).sendMessage(SEND_RESIGN, "");
            case SEND_DISCONNECTED -> {
                if (!clients.get(PLAYER_1).isConnected()) {
                    clients.get(PLAYER_2).sendMessage(SEND_DISCONNECTED, "");
                } else if (!clients.get(PLAYER_2).isConnected()) {
                    clients.get(PLAYER_1).sendMessage(SEND_DISCONNECTED, "");
                }
            }
            default -> {
                clients.get(PLAYER_1).sendMessage(PLAYER_WINNER, winner + "," + score);
                clients.get(PLAYER_2).sendMessage(PLAYER_WINNER, winner + "," + score);
            }
        }

        isGameInProgress = false;
        goban = null;
        player = 1;
        passCount = 0;
        clients.get(PLAYER_1).getSocket().close();
        clients.get(PLAYER_2).getSocket().close();
    }


    private static void changePlayerTurn() {
        player = player == PLAYER_1 ? PLAYER_2 : PLAYER_1;
    }

    private static int getEnemyPlayer() {
        return player == PLAYER_1 ? PLAYER_2 : PLAYER_1;
    }

    @SneakyThrows
    private static String getCapturedStonesJson(List<Movement> movements) {
        CapturedStonesHolder capturedStonesHolder = new CapturedStonesHolder(movements.toArray(new Movement[0]));
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(capturedStonesHolder);
    }

    private static Movement getMovementFromBody(String body) {
        String[] split = body.split(",");
        return new Movement(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    private static Message parseMessage(String message) {
        String[] split = message.split(" ");
        if (isNumeric(split[0])) {
            byte type = Byte.parseByte(split[0]);
            if (type >= START_GAME && type <= RESIGN) {
                String body = split.length > 1 ? split[1] : "";
                return new Message(Byte.parseByte(split[0]), body);
            }
        }
        return null;
    }

    public static boolean isNumeric(String str) {
        try {
            Byte.parseByte(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
package ru.itis.core;

import javafx.scene.paint.Color;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.*;

@Data
@AllArgsConstructor
@Builder
public class Goban {

    protected int player = PLAYER_1;
    private Score score;
    private Set<Stone> capturedStones = new HashSet<>();

    public static final int GRID_SIZE = 5;
    public static final int NO_PLAYER = 0;
    public static final int PLAYER_1 = 1;
    public static final int PLAYER_2 = 2;
    public static final Color PlAYER_1_COLOR = Color.BLACK;
    public static final Color PlAYER_2_COLOR = Color.WHITE;
    private Stone[][] stones = new Stone[GRID_SIZE][GRID_SIZE];

    public Goban() {
        score = new Score();

        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                stones[x][y] = new Stone(x, y);
            }
        }
    }

    public boolean checkTurnAccess(Movement movement) {
        Stone stone = stones[movement.getX()][movement.getY()];
        if (stone.isBlocked()) {
            return false;
        } else return potentialDamesCount(stone) != 0;
    }

    public List<Movement> makeTurn(Movement movement) {
        Stone stone = stones[movement.getX()][movement.getY()];
        Chain chain = createChain(stone);

        score.addPointsToPlayer(capturedStones.size(), player);
        stone.setChain(chain);
        Chain.updateChainInStones(chain);
        stone.setBlocked(true);
        stone.setColor(player == PLAYER_1 ? PlAYER_1_COLOR : PlAYER_2_COLOR);
        stone.setPlayer(player);
        reduceNeighborsDamesCount(getNeighbors(stone), stone);
        changePlayer();

        List<Movement> capturedStonesCopy = new ArrayList<>();
        capturedStones.forEach(s -> capturedStonesCopy.add(new Movement(s)));
        capturedStones = new HashSet<>();
        return capturedStonesCopy;
    }

    private Chain createChain(Stone stone) {
        List<Stone> neighbors = getNeighbors(stone);
        Chain chain = stone.getChain();
        chain.setPlayer(player);

        for (Stone neighbor : neighbors) {
            Chain neighborChain = neighbor.getChain();
            if (neighbor.getPlayer() == NO_PLAYER) {
                chain.addDame(neighbor);
            } else if (neighbor.getPlayer() == player) {
                chain = Chain.mergeChains(chain, neighborChain);
            } else if (neighborChain.isChainCaptured(stone, player)) {
                neighborChain.removeChain();
                neighborChain.getStones().forEach(this::addDameToNeighbors);
                capturedStones.addAll(neighborChain.getStones());
                chain.addDame(neighbor);
            }
        }

        chain.removeDame(stone);
        return chain;
    }

    private int potentialDamesCount(Stone stone) {
        Set<Stone> dames = new HashSet<>();
        List<Stone> neighbors = getNeighbors(stone);

        for (Stone neighbor : neighbors) {
            if (neighbor.getPlayer() == NO_PLAYER || neighbor.getChain().isChainCaptured(stone, player)) {
                dames.add(neighbor);
            } else if (neighbor.getPlayer() == player) {
                dames.addAll(neighbor.getChain().getDames());
            }
        }
        dames.remove(stone);

        return dames.size();
    }

    public List<Stone> getNeighbors(Stone stone) {
        List<Stone> neighbors = new ArrayList<>(4);

        for (int i = 0; i < 4; i++) {
            int x = stone.getX();
            int y = stone.getY();

            switch (i) {
                case 0 -> x--;
                case 1 -> x++;
                case 2 -> y--;
                case 3 -> y++;
            }

           if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
               neighbors.add(stones[x][y]);
           }
        }

        return neighbors;
    }

    public void reduceNeighborsDamesCount(List<Stone> neighbors, Stone stone) {
        int enemyPlayer = player == PLAYER_1 ? PLAYER_2 : PLAYER_1;
        for (Stone neighbor : neighbors) {
            if (neighbor.getPlayer() == enemyPlayer) {
                neighbor.getChain().removeDame(stone);
            }
        }
    }

    public void changePlayer() {
        player = player == PLAYER_1 ? PLAYER_2 : PLAYER_1;
    }

    private void addDameToNeighbors(Stone stone) {
        List<Stone> neighbors = getNeighbors(stone);

        for (Stone neighbor : neighbors) {
            if (neighbor.getPlayer() != NO_PLAYER) {
                neighbor.getChain().addDame(stone);
            }
        }
    }

    public Integer getScore() {
        return score.getScore();
    }

    public int getWinnerPlayer() {
        return score.getWinnerPlayer();
    }
}

package ru.itis.core;

import lombok.Data;

import static ru.itis.core.Goban.PLAYER_1;
import static ru.itis.core.Goban.PLAYER_2;

@Data
class Score {
    private int player1score = 0;
    private int player2score = 0;

    public void addPointsToPlayer(int points, int player) {
        System.out.println("add score: " + points + " " + player);
        if (player == PLAYER_1) {
            player1score += points;
        } else if (player == PLAYER_2) {
            player2score += points;
        }
    }

    public Integer getScore() {
        return Math.abs(player1score - player2score);
    }

    public int getWinnerPlayer() {
        return player1score > player2score ? PLAYER_1 : PLAYER_2;
    }
}

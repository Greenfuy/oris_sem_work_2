package ru.itis.core;

import javafx.scene.paint.Color;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chain {

    private List<Stone> stones = new ArrayList<>();
    private Set<Stone> dames = new HashSet<>();
    private int player;

    private Chain(Stone stone) {
        stones.add(stone);
    }

    public void addStone(Stone stone) {
        stones.add(stone);
    }

    public int getDamesCount() {
        return dames.size();
    }

    public void removeDame(Stone stone) {
        dames.remove(stone);
    }

    public static Chain mergeChains(Chain chain1, Chain chain2) {
        if (chain1.getPlayer() != chain2.getPlayer()) {
            return null;
        }

        Chain mergedChain = new Chain();
        mergedChain.setPlayer(chain1.getPlayer());
        mergedChain.getStones().addAll(chain1.getStones());
        mergedChain.getStones().addAll(chain2.getStones());
        chain1.getDames().forEach(mergedChain::addDame);
        chain2.getDames().forEach(mergedChain::addDame);
        return mergedChain;
    }

    public static void updateChainInStones(Chain chain) {
        for (Stone stone : chain.getStones()) {
            stone.setChain(chain);
        }
    }

    public boolean isChainCaptured(Stone stone, int enemyPlayer) {
        return player != enemyPlayer && getDamesCount() == 1 && dames.contains(stone);
    }

    public void removeChain() {
        removeChain(this);
    }

    public int getStonesCount() {
        return stones.size();
    }

    public void addDame(Stone neighbor) {
        if (neighbor.getPlayer() == 0) {
            dames.add(neighbor);
        }
    }

    private static void removeChain(Chain chain) {
        chain.getStones().forEach(stone -> {
            stone.setPlayer(0);
            stone.setChain(new Chain(stone));
            stone.setBlocked(false);
            stone.setColor(Color.TRANSPARENT);
        });
    }

}

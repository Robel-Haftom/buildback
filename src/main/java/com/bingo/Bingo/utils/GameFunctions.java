package com.bingo.Bingo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GameFunctions {

    public static List<List<Integer>> generateBingoCard() {
        Random rand = new Random();
        List<List<Integer>> card = new ArrayList<>();
        int[][] ranges = {{1,15},{16,30},{31,45},{46,60},{61,75}};

        for (int col = 0; col < 5; col++) {
            List<Integer> colNums =
                    rand.ints(ranges[col][0], ranges[col][1] + 1)
                            .distinct()
                            .limit(5)
                            .boxed()
                            .toList();
            for (int row = 0; row < 5; row++) {
                if (card.size() <= row) card.add(new ArrayList<>());
                card.get(row).add(colNums.get(row));
            }
        }
        // Free space in center
        card.get(2).set(2, 0);
        return card;
    }

}

package com.bingo.Bingo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.List;
import com.bingo.Bingo.dto.response.PlayerInfo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionResponse {
    private String sessionCode;
    private String phase;
    private Integer countdown;
    private Boolean gameActive;
    private Set<Integer> calledNumbers;
    private List<Integer> calledNumbersOrdered;
    private Integer currentCall;
    private String currentCallWithLetter; // Current call with letter prefix (e.g., "B-12")
    private Integer playerCount;
    private String winner;
    private List<Integer> selectedCardCodes; // Cards already taken in this session
    private List<List<Integer>> winningCardNumbers; // Winner's card numbers to display
    private Boolean hasSelectedCard; // Whether the current user has selected a card
    private String waitMessage; // Message to show if user hasn't selected a card
    private Boolean gameInProgress; // Whether the game is already in progress (late joins not allowed)
    private List<PlayerInfo> players; // List of active players with their information
}

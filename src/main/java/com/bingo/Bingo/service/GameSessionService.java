package com.bingo.Bingo.service;

import com.bingo.Bingo.dto.request.JoinGameRequest;
import com.bingo.Bingo.dto.request.BingoCallRequest;
import com.bingo.Bingo.dto.response.GameSessionResponse;
import com.bingo.Bingo.dto.response.BingoCardsResponse;

import java.util.List;

public interface GameSessionService {
    
    List<BingoCardsResponse> generateBingoCard();
    
    GameSessionResponse joinGame(JoinGameRequest request);
    
    GameSessionResponse getGameSession(String sessionCode);
    
    GameSessionResponse getGameSessionWithUser(String sessionCode, Long telegramId);
    
    GameSessionResponse getActiveGameSession();
    
    GameSessionResponse getNextGameInfo();
    
    GameSessionResponse callBingo(BingoCallRequest request);
    
    void startNumberCalling(String sessionCode);
    
    void endGame(String sessionCode);
    
    GameSessionResponse restartGame();
    
    GameSessionResponse getDetailedGameStatus();
    
    GameSessionResponse checkForWinner(String sessionCode);
}

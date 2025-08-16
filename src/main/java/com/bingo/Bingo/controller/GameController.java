package com.bingo.Bingo.controller;

import com.bingo.Bingo.dto.request.JoinGameRequest;
import com.bingo.Bingo.dto.request.BingoCallRequest;
import com.bingo.Bingo.dto.response.BingoCardsResponse;
import com.bingo.Bingo.dto.response.GameSessionResponse;
import com.bingo.Bingo.service.GameSessionService;
import com.bingo.Bingo.utils.GameFunctions;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameSessionService gameSessionService;

    @GetMapping("/start")
    public ResponseEntity<List<BingoCardsResponse>> startGame() {
        return ResponseEntity.ok(gameSessionService.generateBingoCard());
    }

    @PostMapping("/join")
    public ResponseEntity<GameSessionResponse> joinGame(@RequestBody JoinGameRequest request) {
        return ResponseEntity.ok(gameSessionService.joinGame(request));
    }

    @GetMapping("/session/{sessionCode}")
    public ResponseEntity<GameSessionResponse> getGameSession(@PathVariable String sessionCode) {
        return ResponseEntity.ok(gameSessionService.getGameSession(sessionCode));
    }

    @GetMapping("/session/{sessionCode}/user/{telegramId}")
    public ResponseEntity<GameSessionResponse> getGameSessionWithUser(
            @PathVariable String sessionCode,
            @PathVariable Long telegramId) {
        return ResponseEntity.ok(gameSessionService.getGameSessionWithUser(sessionCode, telegramId));
    }

    @GetMapping("/active")
    public ResponseEntity<GameSessionResponse> getActiveGameSession() {
        return ResponseEntity.ok(gameSessionService.getActiveGameSession());
    }

    @GetMapping("/next")
    public ResponseEntity<GameSessionResponse> getNextGameInfo() {
        return ResponseEntity.ok(gameSessionService.getNextGameInfo());
    }

    @PostMapping("/restart")
    public ResponseEntity<GameSessionResponse> restartGame() {
        return ResponseEntity.ok(gameSessionService.restartGame());
    }

    @GetMapping("/status")
    public ResponseEntity<GameSessionResponse> getDetailedGameStatus() {
        return ResponseEntity.ok(gameSessionService.getDetailedGameStatus());
    }

    @PostMapping("/bingo")
    public ResponseEntity<GameSessionResponse> callBingo(@RequestBody BingoCallRequest request) {
        return ResponseEntity.ok(gameSessionService.callBingo(request));
    }

    @PostMapping("/start-calling/{sessionCode}")
    public ResponseEntity<String> startNumberCalling(@PathVariable String sessionCode) {
        gameSessionService.startNumberCalling(sessionCode);
        return ResponseEntity.ok("Number calling started for session: " + sessionCode);
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Backend is working! Game session service is ready.");
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello, welcome to the Bingo game!";
    }
    
    @GetMapping("/debug/players/{sessionCode}")
    public ResponseEntity<Map<String, Object>> getPlayerDebugInfo(@PathVariable String sessionCode) {
        try {
            GameSessionResponse session = gameSessionService.getGameSession(sessionCode);
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("sessionCode", sessionCode);
            debugInfo.put("playerCount", session.getPlayerCount());
            debugInfo.put("phase", session.getPhase());
            debugInfo.put("selectedCardCodes", session.getSelectedCardCodes());
            debugInfo.put("gameActive", session.getGameActive());
            debugInfo.put("countdown", session.getCountdown());
            debugInfo.put("hasSelectedCard", session.getHasSelectedCard());
            debugInfo.put("waitMessage", session.getWaitMessage());
            debugInfo.put("gameInProgress", session.getGameInProgress());
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
    
    @GetMapping("/debug/active-session")
    public ResponseEntity<Map<String, Object>> getActiveSessionDebugInfo() {
        try {
            GameSessionResponse session = gameSessionService.getActiveGameSession();
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("sessionCode", session.getSessionCode());
            debugInfo.put("playerCount", session.getPlayerCount());
            debugInfo.put("phase", session.getPhase());
            debugInfo.put("selectedCardCodes", session.getSelectedCardCodes());
            debugInfo.put("gameActive", session.getGameActive());
            debugInfo.put("countdown", session.getCountdown());
            debugInfo.put("hasSelectedCard", session.getHasSelectedCard());
            debugInfo.put("waitMessage", session.getWaitMessage());
            debugInfo.put("gameInProgress", session.getGameInProgress());
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
    
    @PostMapping("/debug/refresh-session/{sessionCode}")
    public ResponseEntity<Map<String, Object>> forceRefreshSession(@PathVariable String sessionCode) {
        try {
            // Force refresh the session data
            GameSessionResponse session = gameSessionService.getGameSession(sessionCode);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Session refreshed successfully");
            response.put("sessionCode", sessionCode);
            response.put("playerCount", session.getPlayerCount());
            response.put("phase", session.getPhase());
            response.put("selectedCardCodes", session.getSelectedCardCodes());
            response.put("players", session.getPlayers());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
    
    @PostMapping("/debug/force-refresh-all")
    public ResponseEntity<Map<String, Object>> forceRefreshAllSessions() {
        try {
            // This will trigger the force refresh of all sessions
            // The actual refresh happens in the service layer
            GameSessionResponse session = gameSessionService.getActiveGameSession();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "All sessions force refreshed successfully");
            response.put("sessionCode", session.getSessionCode());
            response.put("playerCount", session.getPlayerCount());
            response.put("phase", session.getPhase());
            response.put("selectedCardCodes", session.getSelectedCardCodes());
            response.put("players", session.getPlayers());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
    
    @GetMapping("/game-status/{sessionCode}")
    public ResponseEntity<Map<String, Object>> getGameStatus(@PathVariable String sessionCode) {
        try {
            GameSessionResponse session = gameSessionService.getGameSession(sessionCode);
            Map<String, Object> response = new HashMap<>();
            response.put("sessionCode", session.getSessionCode());
            response.put("phase", session.getPhase());
            response.put("gameActive", session.getGameActive());
            response.put("playerCount", session.getPlayerCount());
            response.put("calledNumbers", session.getCalledNumbers());
            response.put("winner", session.getWinner());
            response.put("winningCardNumbers", session.getWinningCardNumbers());
            response.put("selectedCardCodes", session.getSelectedCardCodes());
            response.put("players", session.getPlayers());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
    
    @PostMapping("/check-winner/{sessionCode}")
    public ResponseEntity<Map<String, Object>> checkForWinner(@PathVariable String sessionCode) {
        try {
            // This will trigger a winner check in the service
            GameSessionResponse session = gameSessionService.checkForWinner(sessionCode);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Winner check completed");
            response.put("sessionCode", sessionCode);
            response.put("hasWinner", session.getWinner() != null);
            response.put("winner", session.getWinner());
            response.put("winningCardNumbers", session.getWinningCardNumbers());
            response.put("phase", session.getPhase());
            response.put("gameActive", session.getGameActive());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
    
    @GetMapping("/debug/cards/{sessionCode}")
    public ResponseEntity<Map<String, Object>> getCardSelectionDebugInfo(@PathVariable String sessionCode) {
        try {
            GameSessionResponse session = gameSessionService.getGameSession(sessionCode);
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("sessionCode", sessionCode);
            debugInfo.put("selectedCardCodes", session.getSelectedCardCodes());
            debugInfo.put("playerCount", session.getPlayerCount());
            debugInfo.put("phase", session.getPhase());
            debugInfo.put("gameActive", session.getGameActive());
            
            // Add card availability info
            List<Integer> allCardCodes = new ArrayList<>();
            for (int i = 1; i <= 400; i++) {
                allCardCodes.add(i);
            }
            List<Integer> availableCards = new ArrayList<>(allCardCodes);
            availableCards.removeAll(session.getSelectedCardCodes());
            
            debugInfo.put("totalCards", 400);
            debugInfo.put("takenCards", session.getSelectedCardCodes());
            debugInfo.put("availableCards", availableCards);
            debugInfo.put("availableCount", availableCards.size());
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
    
    @GetMapping("/available-cards/{sessionCode}")
    public ResponseEntity<Map<String, Object>> getAvailableCards(@PathVariable String sessionCode) {
        try {
            GameSessionResponse session = gameSessionService.getGameSession(sessionCode);
            Map<String, Object> response = new HashMap<>();
            
            // Get all available card codes (1-400)
            List<Integer> allCardCodes = new ArrayList<>();
            for (int i = 1; i <= 400; i++) {
                allCardCodes.add(i);
            }
            
            // Remove already taken cards
            List<Integer> availableCards = new ArrayList<>(allCardCodes);
            if (session.getSelectedCardCodes() != null) {
                availableCards.removeAll(session.getSelectedCardCodes());
            }
            
            response.put("sessionCode", sessionCode);
            response.put("availableCards", availableCards);
            response.put("availableCount", availableCards.size());
            response.put("takenCount", session.getSelectedCardCodes() != null ? session.getSelectedCardCodes().size() : 0);
            response.put("totalCards", 400);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
    
    @GetMapping("/random-available-card/{sessionCode}")
    public ResponseEntity<Map<String, Object>> getRandomAvailableCard(@PathVariable String sessionCode) {
        try {
            GameSessionResponse session = gameSessionService.getGameSession(sessionCode);
            Map<String, Object> response = new HashMap<>();
            
            // Get all available card codes (1-400)
            List<Integer> allCardCodes = new ArrayList<>();
            for (int i = 1; i <= 400; i++) {
                allCardCodes.add(i);
            }
            
            // Remove already taken cards
            List<Integer> availableCards = new ArrayList<>(allCardCodes);
            if (session.getSelectedCardCodes() != null) {
                availableCards.removeAll(session.getSelectedCardCodes());
            }
            
            if (availableCards.isEmpty()) {
                response.put("error", "No cards available");
                response.put("availableCount", 0);
                return ResponseEntity.ok(response);
            }
            
            // Select a random available card
            int randomIndex = (int) (Math.random() * availableCards.size());
            Integer randomCard = availableCards.get(randomIndex);
            
            response.put("sessionCode", sessionCode);
            response.put("randomCard", randomCard);
            response.put("availableCount", availableCards.size());
            response.put("takenCount", session.getSelectedCardCodes() != null ? session.getSelectedCardCodes().size() : 0);
            response.put("totalCards", 400);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
    
    @GetMapping("/card/{cardCode}")
    public ResponseEntity<Map<String, Object>> getCardData(@PathVariable Integer cardCode) {
        try {
            if (cardCode < 1 || cardCode > 400) {
                Map<String, Object> errorInfo = new HashMap<>();
                errorInfo.put("error", "Card code must be between 1 and 400");
                return ResponseEntity.badRequest().body(errorInfo);
            }
            
            // Generate the card data for the requested card code
            // For now, we'll use the basic method - in production, you'd want deterministic card generation
            List<List<Integer>> cardNumbers = GameFunctions.generateBingoCard();
            
            Map<String, Object> response = new HashMap<>();
            response.put("cardCode", cardCode);
            response.put("cardNumbers", cardNumbers);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
}

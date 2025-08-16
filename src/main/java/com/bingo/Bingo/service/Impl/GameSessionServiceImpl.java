package com.bingo.Bingo.service.Impl;

import com.bingo.Bingo.dto.request.JoinGameRequest;
import com.bingo.Bingo.dto.request.BingoCallRequest;
import com.bingo.Bingo.dto.response.GameSessionResponse;
import com.bingo.Bingo.dto.response.BingoCardsResponse;
import com.bingo.Bingo.dto.response.PlayerInfo;
import com.bingo.Bingo.entity.GameSession;
import com.bingo.Bingo.entity.PlayerGameSession;
import com.bingo.Bingo.entity.User;
import com.bingo.Bingo.repository.GameSessionRepository;
import com.bingo.Bingo.repository.PlayerGameSessionRepository;
import com.bingo.Bingo.repository.UserRepository;
import com.bingo.Bingo.service.GameSessionService;

import com.bingo.Bingo.utils.GameFunctions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.InitializingBean;
import com.bingo.Bingo.common.enums.GameStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameSessionServiceImpl implements GameSessionService, InitializingBean {

    private final GameSessionRepository gameSessionRepository;
    private final PlayerGameSessionRepository playerGameSessionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    
    // In-memory storage for active game sessions
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final Map<String, ScheduledFuture<?>> scheduledCallingTasks = new ConcurrentHashMap<>();
    private final Map<String, List<BingoCardsResponse>> sessionCards = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> sessionSelectedCardCodes = new ConcurrentHashMap<>();
    private final Map<String, List<Integer>> sessionCalledNumbersOrdered = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> sessionCountdownTasks = new ConcurrentHashMap<>();

    @Override
    public List<BingoCardsResponse> generateBingoCard() {
        try {
            log.info("Generating 400 unique BINGO cards");
            
            List<BingoCardsResponse> cards = new ArrayList<>();
            Set<String> cardSignatures = new HashSet<>(); // To ensure uniqueness
            
            // Generate 400 unique BINGO cards
            for (int i = 0; i < 400; i++) {
                try {
                    List<List<Integer>> cardNumbers = GameFunctions.generateBingoCard();
                    
                    // Create a signature for this card to check uniqueness
                    String cardSignature = createCardSignature(cardNumbers);
                    
                    // If this card is a duplicate, try to generate a new one
                    int attempts = 0;
                    while (cardSignatures.contains(cardSignature) && attempts < 20) {
                        cardNumbers = GameFunctions.generateBingoCard();
                        cardSignature = createCardSignature(cardNumbers);
                        attempts++;
                    }
                    
                    // If we still have a duplicate after 20 attempts, try a different approach
                    if (cardSignatures.contains(cardSignature)) {
                        // Try to modify the card slightly to make it unique
                        cardNumbers = modifyCardToMakeUnique(cardNumbers, cardSignatures);
                        cardSignature = createCardSignature(cardNumbers);
                        
                        // If still duplicate, skip this card
                        if (cardSignatures.contains(cardSignature)) {
                            log.warn("Could not generate unique card after modifications, skipping card {}", i + 1);
                            continue;
                        }
                    }
                    
                    // Add the signature to our set and create the card
                    cardSignatures.add(cardSignature);
                    
                    Integer cardCode = i + 1;
                    BingoCardsResponse card = BingoCardsResponse.builder()
                            .cardCode(cardCode)
                            .cardNumbers(cardNumbers)
                            .build();
                    
                    cards.add(card);
                    
                    // Log progress every 50 cards
                    if ((i + 1) % 50 == 0) {
                        log.info("Generated {} unique BINGO cards so far", cards.size());
                    }
                    
                } catch (Exception e) {
                    log.error("Error generating card {}: {}", i + 1, e.getMessage(), e);
                    // Continue with other cards
                }
            }
            
            // If we couldn't generate 400 unique cards, try to generate more to reach the target
            if (cards.size() < 400) {
                log.info("Only generated {} unique cards, attempting to generate more to reach 400", cards.size());
                
                int additionalAttempts = 0;
                int maxAdditionalAttempts = 200; // Try up to 200 more attempts
                
                while (cards.size() < 400 && additionalAttempts < maxAdditionalAttempts) {
                    try {
                        List<List<Integer>> cardNumbers = GameFunctions.generateBingoCard();
                        String cardSignature = createCardSignature(cardNumbers);
                        
                        if (!cardSignatures.contains(cardSignature)) {
                            cardSignatures.add(cardSignature);
                            
                            Integer cardCode = cards.size() + 1;
                            BingoCardsResponse card = BingoCardsResponse.builder()
                                    .cardCode(cardCode)
                                    .cardNumbers(cardNumbers)
                                    .build();
                            
                            cards.add(card);
                            
                            if (cards.size() % 50 == 0) {
                                log.info("Generated {} unique BINGO cards so far", cards.size());
                            }
                        }
                        
                        additionalAttempts++;
                        
                    } catch (Exception e) {
                        log.error("Error generating additional card: {}", e.getMessage(), e);
                        additionalAttempts++;
                    }
                }
                
                log.info("After additional attempts, generated {} unique BINGO cards", cards.size());
            }
            
            log.info("Successfully generated {} unique BINGO cards out of 400 requested", cards.size());
            return cards;
            
        } catch (Exception e) {
            log.error("Error generating BINGO cards: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate BINGO cards", e);
        }
    }
    
    /**
     * Create a unique signature for a BINGO card to check for duplicates.
     * This method converts the card numbers into a string representation.
     */
    private String createCardSignature(List<List<Integer>> cardNumbers) {
        if (cardNumbers == null || cardNumbers.isEmpty()) {
            return "empty";
        }
        
        StringBuilder signature = new StringBuilder();
        for (List<Integer> row : cardNumbers) {
            if (row != null) {
                for (Integer num : row) {
                    signature.append(num != null ? num : "null").append(",");
                }
            }
        }
        return signature.toString();
    }
    
    /**
     * Modify a BINGO card to make it unique by slightly changing some numbers.
     * This helps ensure we can generate 400 unique cards.
     */
    private List<List<Integer>> modifyCardToMakeUnique(List<List<Integer>> originalCard, Set<String> existingSignatures) {
        if (originalCard == null || originalCard.isEmpty()) {
            return originalCard;
        }
        
        List<List<Integer>> modifiedCard = new ArrayList<>();
        Random random = new Random();
        
        // Deep copy the original card
        for (List<Integer> row : originalCard) {
            List<Integer> newRow = new ArrayList<>(row);
            modifiedCard.add(newRow);
        }
        
        // Try to modify a few numbers to make it unique
        int modificationAttempts = 0;
        String newSignature;
        
        do {
            // Reset to original
            for (int i = 0; i < originalCard.size(); i++) {
                for (int j = 0; j < originalCard.get(i).size(); j++) {
                    modifiedCard.get(i).set(j, originalCard.get(i).get(j));
                }
            }
            
            // Modify 2-3 random numbers (excluding the center free space)
            int modifications = random.nextInt(2) + 2; // 2 or 3 modifications
            
            for (int mod = 0; mod < modifications; mod++) {
                int row = random.nextInt(5);
                int col = random.nextInt(5);
                
                // Skip the center position (free space)
                if (row == 2 && col == 2) {
                    continue;
                }
                
                // Get the current number and its range
                int currentNum = modifiedCard.get(row).get(col);
                int[][] numberRanges = {{1,15}, {16,30}, {31,45}, {46,60}, {61,75}};
                int min = numberRanges[col][0];
                int max = numberRanges[col][1];
                
                // Generate a new number in the same range
                int newNum;
                do {
                    newNum = random.nextInt(max - min + 1) + min;
                } while (newNum == currentNum);
                
                modifiedCard.get(row).set(col, newNum);
            }
            
            newSignature = createCardSignature(modifiedCard);
            modificationAttempts++;
            
        } while (existingSignatures.contains(newSignature) && modificationAttempts < 10);
        
        return modifiedCard;
    }

    @Override
    public GameSessionResponse joinGame(JoinGameRequest request) {
        try {
            // Get or create active game session
            GameSession gameSession = getOrCreateActiveSession();
            
            // Check if game is already in progress (gameRoom phase) or if countdown has started
            if ("gameRoom".equals(gameSession.getPhase()) || gameSession.getGameActive()) {
                // Game is already in progress, player must wait for next game
                log.info("User {} tried to join game session {} but game is already in progress", 
                         request.getTelegramId(), gameSession.getSessionCode());
                
                // Return a response indicating the game is in progress
                return buildGameSessionResponse(gameSession, null);
            }
            
            // Check if countdown has already started and is below a certain threshold
            if (gameSession.getCountdown() != null && gameSession.getCountdown() < 10) {
                log.info("User {} tried to join game session {} but countdown is too low ({} seconds)", 
                         request.getTelegramId(), gameSession.getSessionCode(), gameSession.getCountdown());
                
                // Return a response indicating the game is about to start
                return buildGameSessionResponse(gameSession, null);
            }
            
            // Ensure consistent phase/countdown state for all players
            if (gameSession.getPhase() == null) {
                gameSession.setPhase("cardSelection");
            }
            if (gameSession.getCountdown() == null || gameSession.getCountdown() <= 0) {
                gameSession.setCountdown(30);
            }
            
            // Find user by telegram ID
            User user = userRepository.findByTelegramId(request.getTelegramId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if the requested card is already taken by another player
            if (request.getSelectedCardCode() != null) {
                // First check the actual player data to see if anyone has this card
                Optional<PlayerGameSession> cardOwner = gameSession.getPlayerSessions() != null ? 
                    gameSession.getPlayerSessions().stream()
                        .filter(p -> p != null && p.getSelectedCardCode() != null && 
                                   p.getSelectedCardCode().equals(request.getSelectedCardCode()))
                        .findFirst() : Optional.empty();
                
                if (cardOwner.isPresent()) {
                    String ownerInfo = "Player ID: " + cardOwner.get().getId() + 
                        (cardOwner.get().getUser() != null ? " (Telegram: " + cardOwner.get().getUser().getTelegramId() + ")" : "Unknown");
                    
                    log.warn("User {} tried to select card {} but it's already taken by: {}", 
                            request.getTelegramId(), request.getSelectedCardCode(), ownerInfo);
                    
                    throw new RuntimeException("Card " + request.getSelectedCardCode() + " is already taken by another player");
                }
                
                // Also check the sessionSelectedCardCodes set as a secondary validation
                Set<Integer> selectedCards = sessionSelectedCardCodes.computeIfAbsent(gameSession.getSessionCode(), k -> ConcurrentHashMap.newKeySet());
                if (selectedCards.contains(request.getSelectedCardCode())) {
                    log.warn("User {} tried to select card {} but it's marked as taken in sessionSelectedCardCodes", 
                            request.getTelegramId(), request.getSelectedCardCode());
                    
                    // Clean up the orphaned card code
                    selectedCards.remove(request.getSelectedCardCode());
                    log.info("Cleaned up orphaned card code {} from sessionSelectedCardCodes", request.getSelectedCardCode());
                }
            }
            
            // Check if user already joined this session
            Optional<PlayerGameSession> existingPlayer = playerGameSessionRepository
                    .findByUserAndGameSession(user, gameSession);
            
            if (existingPlayer.isPresent()) {
                PlayerGameSession player = existingPlayer.get();
                
                // Get the previous card code before updating
                Integer previousCardCode = player.getSelectedCardCode();
                
                // Update existing player session
                if (request.getSelectedCardCode() != null) {
                    // Check if they're trying to select a different card that's already taken
                    if (!request.getSelectedCardCode().equals(previousCardCode)) {
                        Set<Integer> selectedCards = sessionSelectedCardCodes.computeIfAbsent(gameSession.getSessionCode(), k -> ConcurrentHashMap.newKeySet());
                        if (selectedCards.contains(request.getSelectedCardCode())) {
                            // Find who has this card
                            Optional<PlayerGameSession> cardOwner = gameSession.getPlayerSessions() != null ? 
                                gameSession.getPlayerSessions().stream()
                                    .filter(p -> p != null && p.getSelectedCardCode() != null && 
                                               p.getSelectedCardCode().equals(request.getSelectedCardCode()) &&
                                               !p.getId().equals(player.getId()))
                                    .findFirst() : Optional.empty();
                            
                            String ownerInfo = cardOwner.isPresent() ? 
                                "Player ID: " + cardOwner.get().getId() + 
                                (cardOwner.get().getUser() != null ? " (Telegram: " + cardOwner.get().getUser().getTelegramId() + ")" : "") : 
                                "Unknown";
                            
                            log.warn("User {} tried to change from card {} to card {} but it's already taken by: {}", 
                                    request.getTelegramId(), previousCardCode, request.getSelectedCardCode(), ownerInfo);
                            
                            throw new RuntimeException("Card " + request.getSelectedCardCode() + " is already taken by another player");
                        }
                    }
                    
                    player.setSelectedCardCode(request.getSelectedCardCode());
                    try {
                        player.setCardNumbersJson(objectMapper.writeValueAsString(request.getCardNumbers()));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error serializing card numbers", e);
                    }
                } else {
                    player.setSelectedCardCode(null);
                    player.setCardNumbersJson(null);
                }
                
                // Save the player first to ensure ID is available
                PlayerGameSession savedPlayer = playerGameSessionRepository.save(player);
                
                // IMPORTANT: Free up the previous card FIRST, regardless of new selection
                if (previousCardCode != null && (request.getSelectedCardCode() == null || !previousCardCode.equals(request.getSelectedCardCode()))) {
                    Set<Integer> selectedCards = sessionSelectedCardCodes.computeIfAbsent(gameSession.getSessionCode(), k -> ConcurrentHashMap.newKeySet());
                    selectedCards.remove(previousCardCode);
                    log.info("Freed up previous card code {} for session {} (player {} changed selection)", 
                            previousCardCode, gameSession.getSessionCode(), request.getTelegramId());
                    
                    // Also remove from any other players who might have this card
                    for (PlayerGameSession otherPlayer : gameSession.getPlayerSessions()) {
                        if (otherPlayer != null && otherPlayer.getId() != null && 
                            !otherPlayer.getId().equals(player.getId()) && 
                            previousCardCode.equals(otherPlayer.getSelectedCardCode())) {
                            otherPlayer.setSelectedCardCode(null);
                            otherPlayer.setCardNumbersJson(null);
                            playerGameSessionRepository.save(otherPlayer);
                            log.info("Cleared conflicting card selection for player {} (had card {})", 
                                    otherPlayer.getId(), previousCardCode);
                        }
                    }
                    
                    // Log state after freeing up card
                    logCardTrackingState(gameSession, "After freeing up previous card");
                }
                
                // Handle player session list management - keep ALL players in the list for debugging
                // Initialize playerSessions if null
                if (gameSession.getPlayerSessions() == null) {
                    gameSession.setPlayerSessions(new ArrayList<>());
                }
                
                // Check if player is already in the list by ID to prevent duplicates
                boolean playerExists = gameSession.getPlayerSessions().stream()
                        .anyMatch(p -> p.getId() != null && p.getId().equals(savedPlayer.getId()));
                
                if (!playerExists) {
                    gameSession.getPlayerSessions().add(savedPlayer);
                    log.info("Existing player {} added to playerSessions for session {} (card: {}, ID: {})", 
                            request.getTelegramId(), gameSession.getSessionCode(), request.getSelectedCardCode(), savedPlayer.getId());
                }
                
                // Handle card selection logic
                if (request.getSelectedCardCode() != null) {
                    // Mark the new selected card as taken for this session
                    Set<Integer> selectedCards = sessionSelectedCardCodes.computeIfAbsent(gameSession.getSessionCode(), k -> ConcurrentHashMap.newKeySet());
                    selectedCards.add(request.getSelectedCardCode());
                    log.info("Marked card code {} as taken for session {} (player {})", 
                            request.getSelectedCardCode(), gameSession.getSessionCode(), request.getTelegramId());
                    
                    // Log state after marking new card
                    logCardTrackingState(gameSession, "After marking new card");
                } else {
                    // Player removed their card selection but stays in the session for debugging
                    log.info("Player {} removed card selection but remains in session {} for debugging", 
                            request.getTelegramId(), gameSession.getSessionCode());
                }
            } else {
                // Create new player session
                PlayerGameSession playerSession = PlayerGameSession.builder()
                        .user(user)
                        .gameSession(gameSession)
                        .selectedCardCode(request.getSelectedCardCode())
                        .build();
                
                try {
                    playerSession.setCardNumbersJson(objectMapper.writeValueAsString(request.getCardNumbers()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Error serializing card numbers", e);
                }
                
                // Save the player session first to get the ID
                PlayerGameSession savedPlayerSession = playerGameSessionRepository.save(playerSession);
                
                // Add to playerSessions for ALL players who join (for debugging purposes)
                if (gameSession.getPlayerSessions() == null) {
                    gameSession.setPlayerSessions(new ArrayList<>());
                }
                
                // Use the saved player session with the generated ID
                // Ensure no duplicate by ID
                boolean playerExists = gameSession.getPlayerSessions().stream()
                        .anyMatch(p -> p.getId() != null && p.getId().equals(savedPlayerSession.getId()));
                
                if (!playerExists) {
                    gameSession.getPlayerSessions().add(savedPlayerSession);
                    log.info("New player {} added to playerSessions for session {} (card: {}, ID: {})", 
                            request.getTelegramId(), gameSession.getSessionCode(), request.getSelectedCardCode(), savedPlayerSession.getId());
                }
                
                // Only mark card as taken if they actually selected one
                if (request.getSelectedCardCode() != null) {
                    // Mark the selected card as taken for this session
                    sessionSelectedCardCodes.computeIfAbsent(gameSession.getSessionCode(), k -> ConcurrentHashMap.newKeySet()).add(request.getSelectedCardCode());
                    log.info("New player {} marked card code {} as taken for session {}", 
                            request.getTelegramId(), request.getSelectedCardCode(), gameSession.getSessionCode());
                }
            }
            
            // Clean up any duplicate references before saving
            cleanupDuplicatePlayerSessions(gameSession);
            
            // Clean up any orphaned card codes
            cleanupOrphanedCardCodes(gameSession);
            
            // Sync player count with selected card codes to ensure accuracy
            syncPlayerCountWithSelectedCards(gameSession);
            
            // Save game session without altering lifecycle; keep broadcast autonomous
            gameSessionRepository.save(gameSession);
            activeSessions.put(gameSession.getSessionCode(), gameSession);
            
            // Ensure countdown is scheduled if not already running
            if ("cardSelection".equals(gameSession.getPhase()) && !sessionCountdownTasks.containsKey(gameSession.getSessionCode())) {
                ensureCountdownScheduled(gameSession);
            }
            
            // Log detailed player join information
            int totalPlayerCount = gameSession.getPlayerSessions() != null ? 
                    (int) gameSession.getPlayerSessions().stream()
                            .filter(p -> p != null && p.getUser() != null)
                            .count() : 0;
            
            int activePlayerCount = gameSession.getPlayerSessions() != null ? 
                    (int) gameSession.getPlayerSessions().stream()
                            .filter(p -> p != null && p.getSelectedCardCode() != null)
                            .count() : 0;
            
            log.info("User {} joined game session {} with card code: {} - Total players: {}, Active players: {}", 
                    user.getTelegramId(), gameSession.getSessionCode(), request.getSelectedCardCode(), 
                    totalPlayerCount, activePlayerCount);
            
            // Log detailed card tracking state for debugging
            logCardTrackingState(gameSession, "After player join");
            
            // Log all players in the session for debugging
            logAllPlayersInSession(gameSession, "After player join");
            
            // Additional debugging: Log the current state of the session after join
            log.info("Session {} state after join - PlayerSessions size: {}, Selected cards: {}", 
                     gameSession.getSessionCode(), 
                     gameSession.getPlayerSessions() != null ? gameSession.getPlayerSessions().size() : 0,
                     sessionSelectedCardCodes.getOrDefault(gameSession.getSessionCode(), new HashSet<>()).size());
            
            // Log each player in the session
            if (gameSession.getPlayerSessions() != null) {
                log.info("Session {} - Current players after join:", gameSession.getSessionCode());
                for (PlayerGameSession player : gameSession.getPlayerSessions()) {
                    if (player != null && player.getUser() != null) {
                        log.info("  - Player {} (ID: {}): Card: {}", 
                                player.getUser().getTelegramId(), 
                                player.getId(),
                                player.getSelectedCardCode() != null ? player.getSelectedCardCode() : "NO CARD");
                    }
                }
            }
            
            return buildGameSessionResponse(gameSession, user);
            
        } catch (Exception e) {
            log.error("Error in joinGame for user {}: {}", request.getTelegramId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public GameSessionResponse getGameSession(String sessionCode) {
        try {
            GameSession gameSession = activeSessions.get(sessionCode);
            if (gameSession == null) {
                throw new RuntimeException("Game session not found");
            }
            
            // Refresh player sessions from database to ensure accuracy
            refreshPlayerSessionsFromDatabase(gameSession);
            
            log.debug("Retrieved game session: {}, phase: {}, players: {}", 
                     sessionCode, gameSession.getPhase(), 
                     gameSession.getPlayerSessions() != null ? gameSession.getPlayerSessions().size() : 0);
            
            return buildGameSessionResponse(gameSession, null);
            
        } catch (Exception e) {
            log.error("Error getting game session {}: {}", sessionCode, e.getMessage(), e);
            throw e;
        }
    }

    public GameSessionResponse getGameSessionWithUser(String sessionCode, Long telegramId) {
        try {
            GameSession gameSession = activeSessions.get(sessionCode);
            if (gameSession == null) {
                throw new RuntimeException("Game session not found");
            }
            
            User user = null;
            if (telegramId != null) {
                try {
                    user = userRepository.findByTelegramId(telegramId)
                            .orElse(null);
                } catch (Exception e) {
                    log.warn("Could not find user with telegram ID: {}", telegramId, e);
                    // User not found, continue with null user
                }
            }
            
            // Refresh player sessions from database to ensure accuracy
            refreshPlayerSessionsFromDatabase(gameSession);
            
            log.debug("Retrieved game session with user: {}, phase: {}, players: {}, user: {}", 
                     sessionCode, gameSession.getPhase(), 
                     gameSession.getPlayerSessions() != null ? gameSession.getPlayerSessions().size() : 0,
                     user != null ? user.getTelegramId() : "null");
            
            return buildGameSessionResponse(gameSession, user);
            
        } catch (Exception e) {
            log.error("Error getting game session with user {}: {}", sessionCode, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public GameSessionResponse getActiveGameSession() {
        try {
            GameSession activeSession = findActiveGameSession();
            if (activeSession == null) {
                // If no active session exists, get the one that should have been created at startup
                activeSession = getOrCreateActiveSession();
            }
            // Ensure countdown is running while in cardSelection
            if ("cardSelection".equals(activeSession.getPhase())) {
                ensureCountdownScheduled(activeSession);
            }
            
            // Force resync of card tracking state to ensure accuracy
            forceCardTrackingResync(activeSession);
            
            // Refresh player sessions from database to ensure accuracy
            refreshPlayerSessionsFromDatabase(activeSession);
            
            log.debug("Retrieved active game session: {}, phase: {}, players: {}", 
                     activeSession.getSessionCode(), activeSession.getPhase(), 
                     activeSession.getPlayerSessions() != null ? activeSession.getPlayerSessions().size() : 0);
            
            // Log all players in the session for debugging
            logAllPlayersInSession(activeSession, "getActiveGameSession");
            
            return buildGameSessionResponse(activeSession, null);
            
        } catch (Exception e) {
            log.error("Error getting active game session: {}", e.getMessage(), e);
            
            // Return a minimal response to prevent complete failure
            return GameSessionResponse.builder()
                    .sessionCode("error")
                    .phase("error")
                    .countdown(0)
                    .gameActive(false)
                    .calledNumbers(new HashSet<>())
                    .calledNumbersOrdered(new ArrayList<>())
                    .currentCall(null)
                    .playerCount(0)
                    .winner(null)
                    .winningCardNumbers(null)
                    .selectedCardCodes(new ArrayList<>())
                    .hasSelectedCard(false)
                    .waitMessage("Error loading game session")
                    .gameInProgress(false)
                    .players(new ArrayList<>())
                    .build();
        }
    }
    
    /**
     * Get information about when the next game will be available.
     * This is useful for players who arrive when a game is in progress.
     */
    public GameSessionResponse getNextGameInfo() {
        try {
            GameSession activeSession = findActiveGameSession();
            if (activeSession == null) {
                // Create a new session if none exists
                activeSession = getOrCreateActiveSession();
                if ("cardSelection".equals(activeSession.getPhase())) {
                    ensureCountdownScheduled(activeSession);
                }
            }
            
            // Force resync of card tracking state to ensure accuracy
            forceCardTrackingResync(activeSession);
            
            // Build response with next game information
            GameSessionResponse response = buildGameSessionResponse(activeSession, null);
            
            // Add specific message for next game availability
            if ("gameRoom".equals(activeSession.getPhase()) || activeSession.getGameActive()) {
                response.setWaitMessage("Current game is in progress. A new game will start automatically when this one ends.");
            } else if (activeSession.getCountdown() != null && activeSession.getCountdown() < 10) {
                response.setWaitMessage("Game is about to start! Please wait for the next round.");
            } else {
                response.setWaitMessage("New game is available! Select a card to join.");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting next game info: {}", e.getMessage(), e);
            
            return GameSessionResponse.builder()
                    .sessionCode("error")
                    .phase("error")
                    .countdown(0)
                    .gameActive(false)
                    .calledNumbers(new HashSet<>())
                    .calledNumbersOrdered(new ArrayList<>())
                    .currentCall(null)
                    .playerCount(0)
                    .winner(null)
                    .winningCardNumbers(null)
                    .selectedCardCodes(new ArrayList<>())
                    .hasSelectedCard(false)
                    .waitMessage("Error loading next game information")
                    .gameInProgress(false)
                    .players(new ArrayList<>())
                    .build();
        }
    }
    
    /**
     * Manually restart the game session. This creates a new game session
     * and cancels any ongoing games.
     */
    public GameSessionResponse restartGame() {
        try {
            log.info("Manually restarting game session");
            
            // End all current sessions
            for (String sessionCode : new ArrayList<>(activeSessions.keySet())) {
                endGame(sessionCode);
            }
            
            // Create a new session
            GameSession newSession = getOrCreateActiveSession();
            if ("cardSelection".equals(newSession.getPhase())) {
                ensureCountdownScheduled(newSession);
                log.info("Manually created new game session: {} and started countdown", newSession.getSessionCode());
            }
            
            return buildGameSessionResponse(newSession, null);
            
        } catch (Exception e) {
            log.error("Error manually restarting game: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to restart game", e);
        }
    }
    
    /**
     * Get detailed game status information including waiting times and next game availability.
     * This provides comprehensive information for players and spectators.
     */
    public GameSessionResponse getDetailedGameStatus() {
        try {
            GameSession activeSession = findActiveGameSession();
            if (activeSession == null) {
                // Create a new session if none exists
                activeSession = getOrCreateActiveSession();
                if ("cardSelection".equals(activeSession.getPhase())) {
                    ensureCountdownScheduled(activeSession);
                }
            }
            
            // Force resync of card tracking state to ensure accuracy
            forceCardTrackingResync(activeSession);
            
            GameSessionResponse response = buildGameSessionResponse(activeSession, null);
            
            // Add detailed status information
            if ("gameRoom".equals(activeSession.getPhase()) || activeSession.getGameActive()) {
                response.setWaitMessage("🎮 Game is currently in progress! You can watch the current game or wait for the next round. A new game will start automatically when this one ends.");
            } else if (activeSession.getCountdown() != null && activeSession.getCountdown() < 10) {
                response.setWaitMessage("⏰ Game is about to start in " + activeSession.getCountdown() + " seconds! Please wait for the next round.");
            } else if (activeSession.getCountdown() != null && activeSession.getCountdown() > 0) {
                response.setWaitMessage("🎯 Game is open for players! Join now with " + activeSession.getCountdown() + " seconds remaining to select your card.");
            } else {
                response.setWaitMessage("🚀 New game is available! Select a card to join and start playing.");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting detailed game status: {}", e.getMessage(), e);
            
            return GameSessionResponse.builder()
                    .sessionCode("error")
                    .phase("error")
                    .countdown(0)
                    .gameActive(false)
                    .calledNumbers(new HashSet<>())
                    .calledNumbersOrdered(new ArrayList<>())
                    .currentCall(null)
                    .playerCount(0)
                    .winner(null)
                    .winningCardNumbers(null)
                    .selectedCardCodes(new ArrayList<>())
                    .hasSelectedCard(false)
                    .waitMessage("Error loading game status. Please try again later.")
                    .gameInProgress(false)
                    .players(new ArrayList<>())
                    .build();
        }
    }
    
    @Override
    public GameSessionResponse checkForWinner(String sessionCode) {
        try {
            log.info("Manually checking for winner in session: {}", sessionCode);
            
            GameSession gameSession = activeSessions.get(sessionCode);
            if (gameSession == null) {
                throw new RuntimeException("Game session not found");
            }
            
            // Refresh player sessions from database to ensure accuracy
            refreshPlayerSessionsFromDatabase(gameSession);
            
            // Check if there's already a winner
            if (gameSession.getWinningPlayer() != null) {
                log.info("Session {} already has a winner: {}", sessionCode, 
                         gameSession.getWinningPlayer().getUser().getFirstName());
                return buildGameSessionResponse(gameSession, null);
            }
            
            // Check for new winners
            if (gameSession.getCalledNumbers() != null && !gameSession.getCalledNumbers().isEmpty()) {
                for (PlayerGameSession player : gameSession.getPlayerSessions()) {
                    if (player != null && player.getUser() != null && player.getSelectedCardCode() != null) {
                        if (verifyBingo(player, gameSession.getCalledNumbers())) {
                            log.info("Found winner during manual check: {} in session {}", 
                                     player.getUser().getFirstName(), sessionCode);
                            
                            // Announce the winner
                            announceWinner(gameSession, player, gameSession.getCurrentCall());
                            
                            // Return the updated session response
                            return buildGameSessionResponse(gameSession, null);
                        }
                    }
                }
            }
            
            log.info("No winner found in session: {}", sessionCode);
            return buildGameSessionResponse(gameSession, null);
            
        } catch (Exception e) {
            log.error("Error checking for winner in session {}: {}", sessionCode, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public GameSessionResponse callBingo(BingoCallRequest request) {
        try {
            // Find the active game session
            GameSession gameSession = findActiveGameSession();
            if (gameSession == null) {
                throw new RuntimeException("No active game session found");
            }
            
            // Find the player
            User user = userRepository.findByTelegramId(request.getTelegramId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            PlayerGameSession player = playerGameSessionRepository
                    .findByUserAndGameSession(user, gameSession)
                    .orElseThrow(() -> new RuntimeException("Player not found in game session"));
            
            log.info("Player {} called BINGO in session {}", user.getTelegramId(), gameSession.getSessionCode());
            
            // Verify BINGO
            if (verifyBingo(player, gameSession.getCalledNumbers())) {
                // Valid BINGO - player wins
                player.setIsWinner(true);
                gameSession.setWinningPlayer(player);
                gameSession.setGameActive(false);
                
                playerGameSessionRepository.save(player);
                
                // Clean up any duplicate references before saving
                cleanupDuplicatePlayerSessions(gameSession);
                
                gameSessionRepository.save(gameSession);
                
                log.info("Player {} won BINGO in session {}", user.getTelegramId(), gameSession.getSessionCode());
                
                return buildGameSessionResponse(gameSession, user);
            } else {
                // False BINGO - player loses
                player.setIsWinner(false);
                playerGameSessionRepository.save(player);
                
                log.info("Player {} called false BINGO in session {}", user.getTelegramId(), gameSession.getSessionCode());
                
                throw new RuntimeException("False BINGO! You lose the game.");
            }
            
        } catch (Exception e) {
            log.error("Error in callBingo for user {}: {}", request.getTelegramId(), e.getMessage(), e);
            throw e;
        }
    }

    private void restartGameSession(String sessionCode) {
        try {
            log.info("Restarting game session: {}", sessionCode);
            
            // Remove the old session
            activeSessions.remove(sessionCode);
            sessionSelectedCardCodes.remove(sessionCode);
            sessionCalledNumbersOrdered.remove(sessionCode);
            
            // Create a new session
            GameSession newSession = getOrCreateActiveSession();
            
            log.info("Restarted game session: {} -> {}", sessionCode, newSession.getSessionCode());
            
        } catch (Exception e) {
            log.error("Error restarting game session {}: {}", sessionCode, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void startNumberCalling(String sessionCode) {
        try {
            log.info("Starting number calling for session: {}", sessionCode);
            
            // Schedule number calling every 3 seconds
            ScheduledFuture<?> numberCalling = scheduler.scheduleAtFixedRate(() -> {
                try {
                    callNextNumber(sessionCode);
                } catch (Exception e) {
                    log.error("Error in number calling scheduler for session {}: {}", sessionCode, e.getMessage(), e);
                }
            }, 0, 3, TimeUnit.SECONDS);
            
            scheduledCallingTasks.put(sessionCode, numberCalling);
            
        } catch (Exception e) {
            log.error("Error starting number calling for session {}: {}", sessionCode, e.getMessage(), e);
        }
    }

    @Override
    public void endGame(String sessionCode) {
        try {
            log.info("Ending game for session: {}", sessionCode);
            
            GameSession gameSession = activeSessions.get(sessionCode);
            if (gameSession != null) {
                gameSession.setGameActive(false);
                gameSession.setPhase("ended");
                
                // Clean up any duplicate references before saving
                cleanupDuplicatePlayerSessions(gameSession);
                
                gameSessionRepository.save(gameSession);
            }
            
            // Cancel number calling task
            ScheduledFuture<?> numberCallingTask = scheduledCallingTasks.remove(sessionCode);
            if (numberCallingTask != null) {
                numberCallingTask.cancel(true);
                log.debug("Cancelled number calling task for session: {}", sessionCode);
            }
            
            // Cancel countdown task
            ScheduledFuture<?> countdownTask = sessionCountdownTasks.remove(sessionCode);
            if (countdownTask != null) {
                countdownTask.cancel(true);
                log.debug("Cancelled countdown task for session: {}", sessionCode);
            }
            
            // Clear session data
            sessionSelectedCardCodes.remove(sessionCode);
            sessionCalledNumbersOrdered.remove(sessionCode);
            
            log.info("Game ended for session: {}", sessionCode);
            
            // Automatically create a new game session for the next round
            try {
                GameSession newSession = getOrCreateActiveSession();
                if ("cardSelection".equals(newSession.getPhase())) {
                    ensureCountdownScheduled(newSession);
                    log.info("Automatically created new game session: {} and started countdown", newSession.getSessionCode());
                }
            } catch (Exception e) {
                log.error("Error creating new game session after ending game: {}", e.getMessage(), e);
            }
            
        } catch (Exception e) {
            log.error("Error ending game for session {}: {}", sessionCode, e.getMessage(), e);
        }
    }

    private GameSession getOrCreateActiveSession() {
        try {
            // First try to find an existing active session
            GameSession existingSession = findActiveGameSession();
            if (existingSession != null) {
                log.debug("Found existing active session: {}", existingSession.getSessionCode());
                return existingSession;
            }
            
            // Create new session if none exists
            log.info("No active session found, creating new one");
            
            GameSession newSession = GameSession.builder()
                    .sessionCode("GAME_" + System.currentTimeMillis())
                    .status(GameStatus.ONGOING)
                    .phase("cardSelection")
                    .countdown(30)
                    .gameActive(false)
                    .playerSessions(new ArrayList<>())
                    .calledNumbers(new HashSet<>())
                    .build();
            
            GameSession savedSession = gameSessionRepository.save(newSession);
            activeSessions.put(savedSession.getSessionCode(), savedSession);
            
            // Initialize session data
            sessionSelectedCardCodes.putIfAbsent(savedSession.getSessionCode(), ConcurrentHashMap.newKeySet());
            sessionCalledNumbersOrdered.putIfAbsent(savedSession.getSessionCode(), Collections.synchronizedList(new ArrayList<>()));
            
            log.info("Created new game session: {}", savedSession.getSessionCode());
            
            return savedSession;
            
        } catch (Exception e) {
            log.error("Error getting or creating active session: {}", e.getMessage(), e);
            
            // Return a minimal session to prevent complete failure
            return GameSession.builder()
                    .sessionCode("ERROR_" + System.currentTimeMillis())
                    .status(GameStatus.ONGOING)
                    .phase("error")
                    .countdown(0)
                    .gameActive(false)
                    .playerSessions(new ArrayList<>())
                    .calledNumbers(new HashSet<>())
                    .build();
        }
    }

    // Initialize game session when server starts
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            log.info("Initializing GameSessionService");
            
            // Create initial game session
            GameSession initialSession = getOrCreateActiveSession();
            
            // Clean up any existing sessions to prevent duplicate entity issues
            cleanupAllActiveSessions();
            
                    // Schedule periodic cleanup to prevent duplicate entity issues
        scheduler.scheduleAtFixedRate(this::cleanupAllActiveSessions, 5, 5, TimeUnit.MINUTES);
        
        // Schedule periodic cleanup of orphaned card codes
        scheduler.scheduleAtFixedRate(this::cleanupAllOrphanedCardCodes, 2, 2, TimeUnit.MINUTES);
            
            // Automatically start the countdown when server starts
            // This ensures the game begins automatically without waiting for players
            if ("cardSelection".equals(initialSession.getPhase())) {
                ensureCountdownScheduled(initialSession);
                log.info("Automatically started countdown for initial game session: {}", initialSession.getSessionCode());
            }
            
            log.info("GameSessionService initialized with session: {} and countdown started", initialSession.getSessionCode());
            
        } catch (Exception e) {
            log.error("Error initializing GameSessionService: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Force a complete resync of the card tracking state for a session.
     * This method rebuilds the selectedCardCodes set from the actual player data.
     */
    private void forceCardTrackingResync(GameSession gameSession) {
        try {
            List<PlayerGameSession> playerSessions = gameSession.getPlayerSessions();
            if (playerSessions == null) {
                playerSessions = new ArrayList<>();
            }
            
            // Rebuild the selected card codes set from actual player data
            Set<Integer> actualSelectedCards = new HashSet<>();
            for (PlayerGameSession player : playerSessions) {
                if (player != null && player.getSelectedCardCode() != null) {
                    actualSelectedCards.add(player.getSelectedCardCode());
                }
            }
            
            // Update the session selected card codes
            sessionSelectedCardCodes.put(gameSession.getSessionCode(), actualSelectedCards);
            
            log.info("Session {} - Force resync completed. Players: {}, Cards: {}", 
                    gameSession.getSessionCode(), playerSessions.size(), actualSelectedCards.size());
            
        } catch (Exception e) {
            log.error("Error during force resync for session: {}", 
                     gameSession.getSessionCode(), e);
        }
    }
    
    /**
     * Log all players currently in the session for debugging purposes.
     */
    private void logAllPlayersInSession(GameSession gameSession, String context) {
        try {
            List<PlayerGameSession> playerSessions = gameSession.getPlayerSessions();
            
            if (playerSessions == null || playerSessions.isEmpty()) {
                log.info("Session {} - No players in session ({})", gameSession.getSessionCode(), context);
                return;
            }
            
            log.info("Session {} - All Players in Session ({}):", gameSession.getSessionCode(), context);
            
            for (PlayerGameSession player : playerSessions) {
                if (player != null && player.getUser() != null) {
                    String cardInfo = player.getSelectedCardCode() != null ? 
                        "Card: " + player.getSelectedCardCode() : "No card selected";
                    
                    log.info("  - Player: {} (Telegram: {}, ID: {}) - {}", 
                            player.getUser().getFirstName(),
                            player.getUser().getTelegramId(),
                            player.getId(),
                            cardInfo);
                } else {
                    log.warn("  - Invalid player session: {}", player);
                }
            }
            
        } catch (Exception e) {
            log.error("Error logging all players in session: {}", 
                     gameSession.getSessionCode(), e);
        }
    }
    
    /**
     * Log the current state of card tracking for debugging purposes.
     */
    private void logCardTrackingState(GameSession gameSession, String context) {
        try {
            Set<Integer> selectedCardCodes = sessionSelectedCardCodes.getOrDefault(gameSession.getSessionCode(), new HashSet<>());
            List<PlayerGameSession> playerSessions = gameSession.getPlayerSessions();
            
            if (playerSessions == null) {
                playerSessions = new ArrayList<>();
            }
            
            // Get actual cards that players have
            Set<Integer> actualSelectedCards = new HashSet<>();
            Map<Integer, String> cardOwners = new HashMap<>();
            
            for (PlayerGameSession player : playerSessions) {
                if (player != null && player.getSelectedCardCode() != null) {
                    actualSelectedCards.add(player.getSelectedCardCode());
                    String ownerInfo = player.getUser() != null ? 
                        "Telegram:" + player.getUser().getTelegramId() : "ID:" + player.getId();
                    cardOwners.put(player.getSelectedCardCode(), ownerInfo);
                }
            }
            
            // Find orphaned cards
            Set<Integer> orphanedCards = new HashSet<>(selectedCardCodes);
            orphanedCards.removeAll(actualSelectedCards);
            
            log.info("Session {} - Card Tracking State ({}) - Players: {}, Tracked Cards: {}, Actual Cards: {}, Orphaned: {}", 
                    gameSession.getSessionCode(), context, playerSessions.size(), 
                    selectedCardCodes.size(), actualSelectedCards.size(), orphanedCards.size());
            
            if (!orphanedCards.isEmpty()) {
                log.warn("Session {} - Orphaned cards: {}", gameSession.getSessionCode(), orphanedCards);
            }
            
            // Log individual card ownership
            for (Integer cardCode : actualSelectedCards) {
                String owner = cardOwners.get(cardCode);
                log.debug("Session {} - Card {} owned by: {}", gameSession.getSessionCode(), cardCode, owner);
            }
            
        } catch (Exception e) {
            log.error("Error logging card tracking state for session: {}", 
                     gameSession.getSessionCode(), e);
        }
    }
    
    /**
     * Clean up orphaned card codes across all active sessions.
     * This method is scheduled to run periodically.
     */
    private void cleanupAllOrphanedCardCodes() {
        try {
            for (GameSession session : activeSessions.values()) {
                if (session != null) {
                    cleanupOrphanedCardCodes(session);
                }
            }
        } catch (Exception e) {
            log.error("Error during periodic cleanup of orphaned card codes", e);
        }
    }
    
    /**
     * Clean up all active sessions to prevent duplicate entity issues.
     * This method should be called periodically or on startup.
     */
    private void cleanupAllActiveSessions() {
        try {
            log.info("Starting cleanup of all active sessions");
            
            // Force refresh all sessions for data consistency
            forceRefreshAllActiveSessions();
            
            for (GameSession session : activeSessions.values()) {
                if (session != null) {
                    cleanupDuplicatePlayerSessions(session);
                }
            }
            log.info("Completed cleanup of all active sessions");
        } catch (Exception e) {
            log.error("Error cleaning up active sessions: {}", e.getMessage(), e);
        }
    }

    private GameSession findActiveGameSession() {
        try {
            // Look for a session that's not ended
            for (GameSession session : activeSessions.values()) {
                if (session != null && session.getSessionCode() != null && 
                    !"ended".equals(session.getPhase()) && 
                    !"error".equals(session.getPhase())) {
                    log.debug("Found active game session: {}", session.getSessionCode());
                    return session;
                }
            }
            
            log.debug("No active game session found");
            return null;
            
        } catch (Exception e) {
            log.error("Error finding active game session: {}", e.getMessage(), e);
            return null;
        }
    }

    private void ensureCountdownScheduled(GameSession gameSession) {
        try {
            if (gameSession == null || gameSession.getSessionCode() == null) {
                log.warn("Cannot schedule countdown: gameSession={}, sessionCode={}", 
                        gameSession != null ? "exists" : "null", 
                        gameSession != null ? gameSession.getSessionCode() : "null");
                return;
            }
            
            String sessionCode = gameSession.getSessionCode();
            
            // Check if countdown is already scheduled
            if (sessionCountdownTasks.containsKey(sessionCode)) {
                log.debug("Countdown already scheduled for session: {}", sessionCode);
                return;
            }
            
            log.info("Scheduling countdown for session: {} with {} seconds", sessionCode, gameSession.getCountdown());
            
            // Schedule countdown task
            ScheduledFuture<?> countdown = scheduler.scheduleAtFixedRate(() -> {
                try {
                    updateCountdown(sessionCode);
                } catch (Exception e) {
                    log.error("Error in countdown scheduler for session {}: {}", sessionCode, e.getMessage(), e);
                }
            }, 0, 1, TimeUnit.SECONDS);
            
            sessionCountdownTasks.put(sessionCode, countdown);
            
        } catch (Exception e) {
            log.error("Error ensuring countdown scheduled for session {}: {}", 
                     gameSession != null ? gameSession.getSessionCode() : "null", e.getMessage(), e);
        }
    }

    private void updateCountdown(String sessionCode) {
        try {
            GameSession session = activeSessions.get(sessionCode);
            if (session == null) {
                log.warn("Session not found for countdown update: {}", sessionCode);
                return;
            }
            if (!"cardSelection".equals(session.getPhase())) {
                // If not in cardSelection phase, cancel the countdown task
                ScheduledFuture<?> task = sessionCountdownTasks.remove(sessionCode);
                if (task != null) {
                    task.cancel(true);
                    log.debug("Cancelled countdown task for session {} as phase is not cardSelection", sessionCode);
                }
                return;
            }

            Integer cd = session.getCountdown();
            if (cd == null) cd = 30;
            if (cd <= 0) {
                session.setPhase("gameRoom");
                session.setGameActive(true);
                session.setCountdown(0);
                
                // Clean up any duplicate references before saving
                cleanupDuplicatePlayerSessions(session);
                
                gameSessionRepository.save(session);
                ScheduledFuture<?> t = sessionCountdownTasks.remove(sessionCode);
                if (t != null) t.cancel(true);
                startNumberCalling(sessionCode);
            } else {
                session.setCountdown(cd - 1);
                
                // Clean up any duplicate references before saving
                cleanupDuplicatePlayerSessions(session);
                
                gameSessionRepository.save(session);
            }
        } catch (Exception e) {
            log.error("Error updating countdown for session {}: {}", sessionCode, e.getMessage(), e);
        }
    }

    private void callNextNumber(String sessionCode) {
        try {
            GameSession gameSession = activeSessions.get(sessionCode);
            if (gameSession == null || !gameSession.getGameActive()) {
                log.debug("Cannot call next number: session={}, gameActive={}", 
                         sessionCode, gameSession != null ? gameSession.getGameActive() : "null");
                return;
            }
            
            // Generate random number between 1-75 that hasn't been called
            List<Integer> availableNumbers = new ArrayList<>();
            for (int i = 1; i <= 75; i++) {
                if (gameSession.getCalledNumbers() != null && !gameSession.getCalledNumbers().contains(i)) {
                    availableNumbers.add(i);
                }
            }
            
            if (availableNumbers.isEmpty()) {
                // All numbers called, end game
                log.info("All numbers called for session {}, ending game", sessionCode);
                endGame(sessionCode);
                return;
            }
            
            // Call random number
            int randomIndex = new Random().nextInt(availableNumbers.size());
            int calledNumber = availableNumbers.get(randomIndex);
            
            // Get the letter prefix for the called number
            String letterPrefix = getLetterPrefix(calledNumber);
            
            if (gameSession.getCalledNumbers() == null) {
                gameSession.setCalledNumbers(new HashSet<>());
            }
            gameSession.getCalledNumbers().add(calledNumber);
            sessionCalledNumbersOrdered.computeIfAbsent(sessionCode, k -> Collections.synchronizedList(new ArrayList<>())).add(calledNumber);
            gameSession.setCurrentCall(calledNumber);
            
            // Clean up any duplicate references before saving
            cleanupDuplicatePlayerSessions(gameSession);
            
            gameSessionRepository.save(gameSession);
            
            log.info("Called number: {}-{} for session: {}", letterPrefix, calledNumber, sessionCode);
            
            // Broadcast the called number to all players
            broadcastNumberCall(calledNumber, gameSession, letterPrefix);
            
            // Don't check for winners automatically - let players call BINGO manually
            // checkForWinner(gameSession, calledNumber);
            
        } catch (Exception e) {
            log.error("Error calling next number for session {}: {}", sessionCode, e.getMessage(), e);
        }
    }
    
    /**
     * Get the letter prefix for a BINGO number (B, I, N, G, O).
     */
    private String getLetterPrefix(int number) {
        if (number >= 1 && number <= 15) return "B";
        if (number >= 16 && number <= 30) return "I";
        if (number >= 31 && number <= 45) return "N";
        if (number >= 46 && number <= 60) return "G";
        if (number >= 61 && number <= 75) return "O";
        return "?"; // Fallback for invalid numbers
    }

    private void broadcastNumberCall(int calledNumber, GameSession gameSession, String letterPrefix) {
        try {
            // Log the called number with letter prefix (broadcasting is now handled by web app)
            List<PlayerGameSession> playerSessions = gameSession.getPlayerSessions();
            // Only count players who have actually selected a card
            long activePlayerCount = playerSessions != null ? 
                playerSessions.stream()
                    .filter(player -> player != null && player.getSelectedCardCode() != null)
                    .count() : 0;
            
            log.info("Called number: {}-{} for {} active players in session: {}", 
                     letterPrefix, calledNumber, activePlayerCount, gameSession.getSessionCode());
            
        } catch (Exception e) {
            log.error("Error broadcasting number call {}-{} for session {}: {}", 
                     letterPrefix, calledNumber, gameSession != null ? gameSession.getSessionCode() : "null", e.getMessage(), e);
        }
    }

    /**
     * Check if any player has won the game after a number is called.
     * If a winner is found, announce them and end the game.
     */
    private void checkForWinner(GameSession gameSession, int calledNumber) {
        try {
            if (gameSession == null || gameSession.getPlayerSessions() == null) {
                log.debug("Cannot check for winner: gameSession={}, playerSessions={}", 
                         gameSession != null ? "exists" : "null",
                         gameSession != null && gameSession.getPlayerSessions() != null ? "exists" : "null");
                return;
            }
            
            log.debug("Checking for winner in session {} after calling number {}", 
                     gameSession.getSessionCode(), calledNumber);
            
            // Check each player for a winning pattern
            for (PlayerGameSession player : gameSession.getPlayerSessions()) {
                if (player != null && player.getUser() != null && player.getSelectedCardCode() != null) {
                    if (verifyBingo(player, gameSession.getCalledNumbers())) {
                        // We have a winner!
                        announceWinner(gameSession, player, calledNumber);
                        return;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error checking for winner in session {}: {}", 
                     gameSession != null ? gameSession.getSessionCode() : "null", e.getMessage(), e);
        }
    }
    
    /**
     * Announce the winner and end the game.
     */
    private void announceWinner(GameSession gameSession, PlayerGameSession winner, int calledNumber) {
        try {
            String winnerName = winner.getUser().getFirstName() != null ? 
                               winner.getUser().getFirstName() : 
                               winner.getUser().getUserName() != null ? 
                               winner.getUser().getUserName() : 
                               "Player " + winner.getUser().getTelegramId();
            
            log.info("🎉 WINNER ANNOUNCED! Player {} (ID: {}) has won the game in session {}!", 
                     winnerName, winner.getUser().getTelegramId(), gameSession.getSessionCode());
            
            // Set the winner in the game session
            gameSession.setWinningPlayer(winner);
            gameSession.setGameActive(false);
            gameSession.setPhase("ended");
            
            // Save the game session with winner information
            gameSessionRepository.save(gameSession);
            
            // Broadcast winner announcement to all players
            broadcastWinnerAnnouncement(gameSession, winner, calledNumber);
            
            // End the game and restart
            endGameWithWinner(gameSession.getSessionCode(), winner);
            
        } catch (Exception e) {
            log.error("Error announcing winner for session {}: {}", 
                     gameSession.getSessionCode(), e.getMessage(), e);
        }
    }
    
    /**
     * Broadcast winner announcement to all players.
     */
    private void broadcastWinnerAnnouncement(GameSession gameSession, PlayerGameSession winner, int calledNumber) {
        try {
            String winnerName = winner.getUser().getFirstName() != null ? 
                               winner.getUser().getFirstName() : 
                               winner.getUser().getUserName() != null ? 
                               winner.getUser().getUserName() : 
                               "Player " + winner.getUser().getTelegramId();
            
            String announcement = String.format(
                "🎉 **BINGO! WE HAVE A WINNER!** 🎉\n\n" +
                "🏆 **Winner:** %s\n" +
                "🎯 **Winning Card:** %d\n" +
                "🔢 **Winning Number:** %d\n" +
                "🎮 **Session:** %s\n\n" +
                "The game will restart automatically in a few seconds!",
                winnerName, winner.getSelectedCardCode(), calledNumber, gameSession.getSessionCode()
            );
            
            log.info("Broadcasting winner announcement: {}", announcement);
            
            // TODO: Implement actual broadcasting to Telegram users
            // For now, just log the announcement
            
        } catch (Exception e) {
            log.error("Error broadcasting winner announcement for session {}: {}", 
                     gameSession.getSessionCode(), e.getMessage(), e);
        }
    }
    
    /**
     * End the game with winner information and restart.
     */
    private void endGameWithWinner(String sessionCode, PlayerGameSession winner) {
        try {
            log.info("Ending game with winner for session: {}", sessionCode);
            
            // End the current game
            endGame(sessionCode);
            
            // Add a delay before starting the new game to allow players to see the announcement
            scheduler.schedule(() -> {
                try {
                    log.info("Starting new game after winner announcement for session: {}", sessionCode);
                    
                    // Create new game session
                    GameSession newSession = getOrCreateActiveSession();
                    if ("cardSelection".equals(newSession.getPhase())) {
                        ensureCountdownScheduled(newSession);
                        log.info("New game session created: {} and countdown started", newSession.getSessionCode());
                    }
                    
                } catch (Exception e) {
                    log.error("Error starting new game after winner: {}", e.getMessage(), e);
                }
            }, 5, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.error("Error ending game with winner for session {}: {}", sessionCode, e.getMessage(), e);
        }
    }
    
    /**
     * Get the winner's card numbers for display.
     */
    private List<List<Integer>> getWinnerCardNumbers(PlayerGameSession winner) {
        try {
            if (winner == null || winner.getCardNumbersJson() == null) {
                log.warn("Cannot get winner card numbers: winner={}, cardNumbersJson={}", 
                        winner != null ? winner.getId() : "null",
                        winner != null ? winner.getCardNumbersJson() : "null");
                return null;
            }
            
            List<List<Integer>> cardNumbers = objectMapper.readValue(
                    winner.getCardNumbersJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, List.class)
            );
            
            log.debug("Retrieved winner card numbers for player {}: {}x{}", 
                     winner.getId(), 
                     cardNumbers != null ? cardNumbers.size() : 0, 
                     cardNumbers != null && !cardNumbers.isEmpty() ? cardNumbers.get(0).size() : 0);
            
            return cardNumbers;
            
        } catch (Exception e) {
            log.error("Error getting winner card numbers for player: {}", winner != null ? winner.getId() : "null", e);
            return null;
        }
    }
    
    private boolean verifyBingo(PlayerGameSession player, Set<Integer> calledNumbers) {
        try {
            if (player == null || player.getCardNumbersJson() == null || calledNumbers == null) {
                log.warn("Invalid parameters for BINGO verification: player={}, cardNumbersJson={}, calledNumbers={}", 
                        player != null ? player.getId() : "null", 
                        player != null ? player.getCardNumbersJson() : "null", 
                        calledNumbers.size());
                return false;
            }
            
            List<List<Integer>> cardNumbers = objectMapper.readValue(
                    player.getCardNumbersJson(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, List.class)
            );
            
            if (cardNumbers == null || cardNumbers.isEmpty()) {
                log.warn("Invalid card numbers for player {}: {}", player.getId(), player.getCardNumbersJson());
                return false;
            }
            
            // Check rows
            for (List<Integer> row : cardNumbers) {
                if (row != null && row.stream().allMatch(cell -> calledNumbers.contains(cell) || cell == 0)) {
                    log.debug("Player {} has winning row: {}", player.getId(), row);
                    return true;
                }
            }
            
            // Check columns
            for (int col = 0; col < 5; col++) {
                boolean columnWin = true;
                for (int row = 0; row < 5; row++) {
                    if (row < cardNumbers.size() && col < cardNumbers.get(row).size()) {
                        int cell = cardNumbers.get(row).get(col);
                        if (!calledNumbers.contains(cell) && cell != 0) {
                            columnWin = false;
                            break;
                        }
                    } else {
                        columnWin = false;
                        break;
                    }
                }
                if (columnWin) {
                    log.debug("Player {} has winning column: {}", player.getId(), col);
                    return true;
                }
            }
            
            // Check diagonals
            boolean diagonal1Win = true;
            boolean diagonal2Win = true;
            
            for (int i = 0; i < 5; i++) {
                if (i < cardNumbers.size() && i < cardNumbers.get(i).size()) {
                    int cell1 = cardNumbers.get(i).get(i);
                    int cell2 = cardNumbers.get(i).get(4 - i);
                    
                    if (!calledNumbers.contains(cell1) && cell1 != 0) diagonal1Win = false;
                    if (!calledNumbers.contains(cell2) && cell2 != 0) diagonal2Win = false;
                } else {
                    diagonal1Win = false;
                    diagonal2Win = false;
                    break;
                }
            }
            
            if (diagonal1Win) {
                log.debug("Player {} has winning diagonal 1", player.getId());
                return true;
            }
            if (diagonal2Win) {
                log.debug("Player {} has winning diagonal 2", player.getId());
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error verifying BINGO for player: {}", player != null ? player.getId() : "null", e);
            return false;
        }
    }

    private GameSessionResponse buildGameSessionResponse(GameSession gameSession, User currentUser) {
        try {
            // Ensure playerSessions is not null
            List<PlayerGameSession> playerSessions = gameSession.getPlayerSessions();
            if (playerSessions == null) {
                playerSessions = new ArrayList<>();
            }
            Set<Integer> selectedCardCodes = sessionSelectedCardCodes.getOrDefault(gameSession.getSessionCode(), new HashSet<>());

            // Count ALL players in the session for debugging purposes
            long totalPlayerCount = playerSessions.stream()
                    .filter(player -> player != null && player.getUser() != null)
                    .count();
            
            // Also count players who have actually selected a card
            long activePlayerCount = playerSessions.stream()
                    .filter(player -> player != null && player.getSelectedCardCode() != null)
                    .count();
            
            // Validate that the count matches the selected card codes set
            Set<Integer> selectedCardCodesSet = sessionSelectedCardCodes.getOrDefault(gameSession.getSessionCode(), new HashSet<>());
            long selectedCardsCount = selectedCardCodesSet.size();
            
            // Log detailed player information for debugging
            log.info("Session {} - Player Count Debug:", gameSession.getSessionCode());
            log.info("  - Total player sessions: {}", playerSessions != null ? playerSessions.size() : 0);
            log.info("  - Total players (registered): {}", totalPlayerCount);
            log.info("  - Active players (with cards): {}", activePlayerCount);
            log.info("  - Selected card codes count: {}", selectedCardsCount);
            
            // Log each player's status with more detail
            if (playerSessions != null) {
                log.info("Session {} - Player Details:", gameSession.getSessionCode());
                for (PlayerGameSession player : playerSessions) {
                    if (player != null && player.getUser() != null) {
                        log.info("  - Player {} (ID: {}, Session ID: {}): Card selected: {}", 
                                player.getUser().getTelegramId(), 
                                player.getId(),
                                player.getGameSession() != null ? player.getGameSession().getId() : "null",
                                player.getSelectedCardCode() != null ? "YES - " + player.getSelectedCardCode() : "NO");
                    } else {
                        log.warn("Session {} - Found null player or user in playerSessions: player={}, user={}", 
                                gameSession.getSessionCode(),
                                player != null ? player.getId() : "null",
                                player != null && player.getUser() != null ? player.getUser().getTelegramId() : "null");
                    }
                }
            } else {
                log.warn("Session {} - playerSessions is null", gameSession.getSessionCode());
            }
            
            // If there's a mismatch, log a warning and sync
            if (activePlayerCount != selectedCardsCount) {
                log.warn("Session {} - Player count mismatch! Active players: {}, Selected cards: {}", 
                        gameSession.getSessionCode(), activePlayerCount, selectedCardsCount);
                
                // Force sync to fix the mismatch
                syncPlayerCountWithSelectedCards(gameSession);
                
                // Recalculate after sync
                selectedCardCodesSet = sessionSelectedCardCodes.getOrDefault(gameSession.getSessionCode(), new HashSet<>());
                selectedCardsCount = selectedCardCodesSet.size();
                log.info("Session {} - After sync: Active players: {}, Selected cards: {}", 
                        gameSession.getSessionCode(), activePlayerCount, selectedCardsCount);
            }

            // Check if current user has selected a card
            Boolean hasSelectedCard = false;
            String waitMessage = null;
            
            if (currentUser != null) {
                Optional<PlayerGameSession> currentPlayer = playerSessions.stream()
                        .filter(player -> player != null && player.getUser() != null && player.getUser().getId() != null && 
                                       currentUser.getId() != null && player.getUser().getId().equals(currentUser.getId()))
                        .findFirst();
                
                if (currentPlayer.isPresent()) {
                    hasSelectedCard = currentPlayer.get().getSelectedCardCode() != null;
                }
                
                if (!hasSelectedCard) {
                    if ("gameRoom".equals(gameSession.getPhase()) || gameSession.getGameActive()) {
                        waitMessage = "Game is already in progress. Please wait for the current game to end before joining.";
                    } else if (gameSession.getCountdown() != null && gameSession.getCountdown() < 10) {
                        waitMessage = "Game is about to start! Please wait for the next round.";
                    } else {
                        waitMessage = "Please select a card to join the game";
                    }
                }
            } else {
                // No current user (spectator or general request)
                if ("gameRoom".equals(gameSession.getPhase()) || gameSession.getGameActive()) {
                    waitMessage = "Game is currently in progress. You can watch or wait for the next round.";
                } else if (gameSession.getCountdown() != null && gameSession.getCountdown() < 10) {
                    waitMessage = "Game is about to start! Please wait for the next round.";
                } else {
                    waitMessage = "Game is open for players to join. Select a card to participate.";
                }
            }

            List<Integer> ordered = sessionCalledNumbersOrdered.getOrDefault(gameSession.getSessionCode(), Collections.emptyList());
            
            // Safely build the response without triggering toString() on entities
            GameSessionResponse response = GameSessionResponse.builder()
                    .sessionCode(gameSession.getSessionCode())
                    .phase(gameSession.getPhase())
                    .countdown(gameSession.getCountdown())
                    .gameActive(gameSession.getGameActive())
                    .calledNumbers(gameSession.getCalledNumbers() != null ? new HashSet<>(gameSession.getCalledNumbers()) : new HashSet<>())
                    .calledNumbersOrdered(new ArrayList<>(ordered))
                    .currentCall(gameSession.getCurrentCall())
                    .currentCallWithLetter(gameSession.getCurrentCall() != null ? 
                            getLetterPrefix(gameSession.getCurrentCall()) + "-" + gameSession.getCurrentCall() : null)
                    .playerCount((int) totalPlayerCount)
                    .winner(gameSession.getWinningPlayer() != null && gameSession.getWinningPlayer().getUser() != null ? 
                            gameSession.getWinningPlayer().getUser().getFirstName() : null)
                    .winningCardNumbers(gameSession.getWinningPlayer() != null ? readCardNumbers(gameSession.getWinningPlayer()) : null)
                    .selectedCardCodes(new ArrayList<>(selectedCardCodesSet))
                    .hasSelectedCard(hasSelectedCard)
                    .waitMessage(waitMessage)
                    .gameInProgress("gameRoom".equals(gameSession.getPhase()) || gameSession.getWinningPlayer() != null)
                    .players(buildPlayerInfoList(gameSession))
                    .build();
            
            log.info("Built game session response for session: {}, totalPlayers: {}, activePlayers: {}, totalPlayerSessions: {}, phase: {}", 
                     gameSession.getSessionCode(), totalPlayerCount, activePlayerCount,
                     gameSession.getPlayerSessions() != null ? gameSession.getPlayerSessions().size() : 0, 
                     gameSession.getPhase());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error building game session response for session: {}", 
                     gameSession != null ? gameSession.getSessionCode() : "null", e);
            
            // Return a minimal response to prevent complete failure
            return GameSessionResponse.builder()
                    .sessionCode(gameSession != null ? gameSession.getSessionCode() : "unknown")
                    .phase("error")
                    .countdown(0)
                    .gameActive(false)
                    .calledNumbers(new HashSet<>())
                    .calledNumbersOrdered(new ArrayList<>())
                    .currentCall(null)
                    .playerCount(0)
                    .winner(null)
                    .winningCardNumbers(null)
                    .selectedCardCodes(new ArrayList<>())
                    .hasSelectedCard(false)
                    .waitMessage("Error loading game session")
                    .gameInProgress(false)
                    .players(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Build a list of PlayerInfo objects from the game session's player sessions.
     */
    private List<PlayerInfo> buildPlayerInfoList(GameSession gameSession) {
        try {
            List<PlayerInfo> playerInfoList = new ArrayList<>();
            
            if (gameSession.getPlayerSessions() != null) {
                log.debug("Session {} - Total PlayerGameSessions: {}", 
                         gameSession.getSessionCode(), gameSession.getPlayerSessions().size());
                
                for (PlayerGameSession playerSession : gameSession.getPlayerSessions()) {
                    if (playerSession != null && playerSession.getUser() != null) {
                        // Include ALL players for debugging, not just those with selected cards
                        PlayerInfo playerInfo = PlayerInfo.builder()
                                .telegramId(playerSession.getUser().getTelegramId())
                                .firstName(playerSession.getUser().getFirstName())
                                .lastName(playerSession.getUser().getLastName())
                                .userName(playerSession.getUser().getUserName())
                                .selectedCardCode(playerSession.getSelectedCardCode())
                                .isWinner(playerSession.getIsWinner())
                                .build();
                        
                        playerInfoList.add(playerInfo);
                        
                        log.debug("Session {} - Added player: {} (Telegram: {}, Card: {}, Winner: {})", 
                                 gameSession.getSessionCode(),
                                 playerSession.getUser().getFirstName(),
                                 playerSession.getUser().getTelegramId(),
                                 playerSession.getSelectedCardCode(),
                                 playerSession.getIsWinner());
                    } else {
                        log.warn("Session {} - Skipping invalid PlayerGameSession: playerSession={}, user={}", 
                                 gameSession.getSessionCode(),
                                 playerSession != null ? playerSession.getId() : "null",
                                 playerSession != null && playerSession.getUser() != null ? 
                                     playerSession.getUser().getTelegramId() : "null");
                    }
                }
            } else {
                log.warn("Session {} - No PlayerGameSessions found", gameSession.getSessionCode());
            }
            
            log.info("Session {} - Built player info list: {} players", 
                     gameSession.getSessionCode(), playerInfoList.size());
            
            return playerInfoList;
            
        } catch (Exception e) {
            log.error("Error building player info list for session: {}", 
                     gameSession.getSessionCode(), e);
            return new ArrayList<>();
        }
    }
    
    private List<List<Integer>> readCardNumbers(PlayerGameSession player) {
        try {
            if (player == null || player.getCardNumbersJson() == null) {
                log.warn("Cannot read card numbers: player={}, cardNumbersJson={}", 
                        player != null ? player.getId() : "null", 
                        player != null ? player.getCardNumbersJson() : "null");
                return null;
            }
            
            List<List<Integer>> cardNumbers = objectMapper.readValue(
                    player.getCardNumbersJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, List.class)
            );
            
            log.debug("Successfully read card numbers for player {}: {}x{}", 
                     player.getId(), 
                     cardNumbers != null ? cardNumbers.size() : 0, 
                     cardNumbers != null && !cardNumbers.isEmpty() ? cardNumbers.get(0).size() : 0);
            
            return cardNumbers;
            
        } catch (Exception e) {
            log.error("Error reading card numbers for player: {}", player != null ? player.getId() : "null", e);
            return null;
        }
    }
    
    /**
     * Clean up duplicate references in the playerSessions list to prevent Hibernate errors.
     * This method removes duplicate PlayerGameSession entities based on their ID.
     */
    private void cleanupDuplicatePlayerSessions(GameSession gameSession) {
        if (gameSession.getPlayerSessions() == null || gameSession.getPlayerSessions().isEmpty()) {
            return;
        }
        
        // Validate the integrity of the playerSessions list first
        validatePlayerSessionsIntegrity(gameSession);
        
        // Log the current state for debugging
        log.debug("Cleaning up player sessions for session: {}, current count: {}", 
                 gameSession.getSessionCode(), gameSession.getPlayerSessions().size());
        
        // Use a Set to track seen IDs and remove duplicates
        Set<Long> seenIds = new HashSet<>();
        List<PlayerGameSession> uniquePlayers = new ArrayList<>();
        
        for (PlayerGameSession player : gameSession.getPlayerSessions()) {
            if (player != null && player.getId() != null) {
                if (!seenIds.contains(player.getId())) {
                    seenIds.add(player.getId());
                    uniquePlayers.add(player);
                } else {
                    log.warn("Removing duplicate PlayerGameSession with ID: {} from session: {}", 
                             player.getId(), gameSession.getSessionCode());
                }
            } else {
                log.warn("Found null or invalid PlayerGameSession in session: {}, player: {}", 
                         gameSession.getSessionCode(), player);
            }
        }
        
        // Replace the list with the deduplicated version
        if (uniquePlayers.size() != gameSession.getPlayerSessions().size()) {
            int removedCount = gameSession.getPlayerSessions().size() - uniquePlayers.size();
            gameSession.setPlayerSessions(uniquePlayers);
            log.info("Cleaned up duplicate player sessions for session: {}, removed {} duplicates", 
                     gameSession.getSessionCode(), removedCount);
        } else {
            log.debug("No duplicates found in session: {}", gameSession.getSessionCode());
        }
    }
    
    /**
     * Validate the integrity of the playerSessions list to ensure data consistency.
     * This method logs warnings for any inconsistencies found.
     */
    private void validatePlayerSessionsIntegrity(GameSession gameSession) {
        if (gameSession.getPlayerSessions() == null) {
            return;
        }
        
        Set<Long> playerIds = new HashSet<>();
        Set<Long> duplicateIds = new HashSet<>();
        
        for (PlayerGameSession player : gameSession.getPlayerSessions()) {
            if (player != null && player.getId() != null) {
                if (!playerIds.add(player.getId())) {
                    duplicateIds.add(player.getId());
                }
            }
        }
        
        if (!duplicateIds.isEmpty()) {
            log.warn("Found duplicate player IDs in session {}: {}", 
                     gameSession.getSessionCode(), duplicateIds);
        }
        
        // Check for null references
        long nullCount = gameSession.getPlayerSessions().stream()
                .filter(Objects::isNull)
                .count();
        
        if (nullCount > 0) {
            log.warn("Found {} null references in playerSessions for session: {}", 
                     nullCount, gameSession.getSessionCode());
        }
    }
    
    /**
     * Force refresh all active sessions to ensure data consistency between memory and database.
     * This method should be called when there are suspected data inconsistencies.
     */
    private void forceRefreshAllActiveSessions() {
        try {
            log.info("Force refreshing all active sessions for data consistency");
            for (GameSession session : activeSessions.values()) {
                refreshPlayerSessionsFromDatabase(session);
            }
            log.info("Completed force refresh of all active sessions");
            
            // Log the current state after refresh
            logCurrentSessionState();
        } catch (Exception e) {
            log.error("Error during force refresh of all active sessions", e);
        }
    }
    
    /**
     * Log the current state of all active sessions for debugging purposes.
     */
    private void logCurrentSessionState() {
        try {
            log.info("=== CURRENT SESSION STATE DEBUG ===");
            for (GameSession session : activeSessions.values()) {
                if (session != null) {
                    log.info("Session: {} (Phase: {}, GameActive: {})", 
                            session.getSessionCode(), session.getPhase(), session.getGameActive());
                    log.info("  - PlayerSessions in memory: {}", 
                            session.getPlayerSessions() != null ? session.getPlayerSessions().size() : 0);
                    log.info("  - Selected card codes in memory: {}", 
                            sessionSelectedCardCodes.getOrDefault(session.getSessionCode(), new HashSet<>()).size());
                    
                    // Log each player
                    if (session.getPlayerSessions() != null) {
                        for (PlayerGameSession player : session.getPlayerSessions()) {
                            if (player != null && player.getUser() != null) {
                                log.info("    - Player {} (ID: {}): Card: {}", 
                                        player.getUser().getTelegramId(), 
                                        player.getId(),
                                        player.getSelectedCardCode() != null ? player.getSelectedCardCode() : "NO CARD");
                            }
                        }
                    }
                }
            }
            log.info("=== END SESSION STATE DEBUG ===");
        } catch (Exception e) {
            log.error("Error logging current session state", e);
        }
    }
    
    /**
     * Clean up any orphaned card codes that are marked as taken but no player actually has them.
     * This method should be called periodically to maintain data consistency.
     */
    private void cleanupOrphanedCardCodes(GameSession gameSession) {
        try {
            Set<Integer> selectedCardCodes = sessionSelectedCardCodes.getOrDefault(gameSession.getSessionCode(), new HashSet<>());
            List<PlayerGameSession> playerSessions = gameSession.getPlayerSessions();
            
            if (playerSessions == null) {
                playerSessions = new ArrayList<>();
            }
            
            // Get actual cards that players have
            Set<Integer> actualSelectedCards = new HashSet<>();
            for (PlayerGameSession player : playerSessions) {
                if (player != null && player.getSelectedCardCode() != null) {
                    actualSelectedCards.add(player.getSelectedCardCode());
                }
            }
            
            // Find orphaned cards
            Set<Integer> orphanedCards = new HashSet<>(selectedCardCodes);
            orphanedCards.removeAll(actualSelectedCards);
            
            if (!orphanedCards.isEmpty()) {
                log.warn("Session {} - Found orphaned card codes: {}", 
                        gameSession.getSessionCode(), orphanedCards);
                
                // Remove orphaned cards
                selectedCardCodes.removeAll(orphanedCards);
                log.info("Session {} - Cleaned up {} orphaned card codes", 
                        gameSession.getSessionCode(), orphanedCards.size());
            }
        } catch (Exception e) {
            log.error("Error cleaning up orphaned card codes for session: {}", 
                     gameSession.getSessionCode(), e);
        }
    }
    
    /**
     * Refresh the player sessions list from the database to ensure the in-memory list is up-to-date.
     * This fixes issues with lazy loading and ensures accurate player counts.
     */
    private void refreshPlayerSessionsFromDatabase(GameSession gameSession) {
        try {
            // Query the database for all player sessions for this game session
            List<PlayerGameSession> dbPlayerSessions = playerGameSessionRepository
                    .findByGameSessionId(gameSession.getId());
            
            if (dbPlayerSessions != null && !dbPlayerSessions.isEmpty()) {
                // Update the in-memory list with the database data
                gameSession.setPlayerSessions(new ArrayList<>(dbPlayerSessions));
                log.debug("Session {} - Refreshed player sessions from database: {} players", 
                         gameSession.getSessionCode(), dbPlayerSessions.size());
                
                // Also refresh the selected card codes set
                Set<Integer> actualSelectedCards = new HashSet<>();
                for (PlayerGameSession player : dbPlayerSessions) {
                    if (player != null && player.getSelectedCardCode() != null) {
                        actualSelectedCards.add(player.getSelectedCardCode());
                    }
                }
                sessionSelectedCardCodes.put(gameSession.getSessionCode(), actualSelectedCards);
                
                log.debug("Session {} - Updated selected card codes from database: {} cards", 
                         gameSession.getSessionCode(), actualSelectedCards.size());
            } else {
                log.debug("Session {} - No player sessions found in database", gameSession.getSessionCode());
                gameSession.setPlayerSessions(new ArrayList<>());
                sessionSelectedCardCodes.put(gameSession.getSessionCode(), new HashSet<>());
            }
        } catch (Exception e) {
            log.error("Error refreshing player sessions from database for session: {}", 
                     gameSession.getSessionCode(), e);
        }
    }
    
    /**
     * Synchronize the player count with the selected card codes to ensure accuracy.
     * This method validates that the player count matches the number of selected cards.
     */
    private void syncPlayerCountWithSelectedCards(GameSession gameSession) {
        try {
            Set<Integer> selectedCardCodes = sessionSelectedCardCodes.getOrDefault(gameSession.getSessionCode(), new HashSet<>());
            List<PlayerGameSession> playerSessions = gameSession.getPlayerSessions();
            
            if (playerSessions == null) {
                playerSessions = new ArrayList<>();
            }
            
            // Count actual active players
            long actualPlayerCount = playerSessions.stream()
                    .filter(player -> player != null && player.getSelectedCardCode() != null)
                    .count();
            
            // Rebuild the selected card codes set from actual player data (always sync)
            Set<Integer> actualSelectedCards = new HashSet<>();
            for (PlayerGameSession player : playerSessions) {
                if (player != null && player.getSelectedCardCode() != null) {
                    actualSelectedCards.add(player.getSelectedCardCode());
                    log.debug("Session {} - Player {} has card {}", 
                            gameSession.getSessionCode(), player.getId(), player.getSelectedCardCode());
                }
            }
            
            // Check for orphaned card codes (cards marked as taken but no player has them)
            Set<Integer> orphanedCards = new HashSet<>(selectedCardCodes);
            orphanedCards.removeAll(actualSelectedCards);
            if (!orphanedCards.isEmpty()) {
                log.warn("Session {} - Found orphaned card codes: {}", 
                        gameSession.getSessionCode(), orphanedCards);
            }
            
            // Update the session selected card codes
            sessionSelectedCardCodes.put(gameSession.getSessionCode(), actualSelectedCards);
            
            log.info("Session {} - Synced selected card codes. Players: {}, Cards: {}, Orphaned: {}", 
                    gameSession.getSessionCode(), actualPlayerCount, actualSelectedCards.size(), orphanedCards.size());
            
            // Validate final count
            if (actualPlayerCount != actualSelectedCards.size()) {
                log.error("Session {} - Final validation failed! Players: {}, Cards: {}", 
                        gameSession.getSessionCode(), actualPlayerCount, actualSelectedCards.size());
            }
        } catch (Exception e) {
            log.error("Error syncing player count with selected cards for session: {}", 
                     gameSession.getSessionCode(), e);
        }
    }
}

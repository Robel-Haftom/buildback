package com.bingo.Bingo.utils;

import com.bingo.Bingo.dto.request.UserDto;
import com.bingo.Bingo.dto.request.JoinGameRequest;
import com.bingo.Bingo.dto.request.BingoCallRequest;
import com.bingo.Bingo.dto.response.GameSessionResponse;
import com.bingo.Bingo.dto.response.BingoCardsResponse;
import com.bingo.Bingo.entity.User;
import com.bingo.Bingo.repository.UserRepository;
import com.bingo.Bingo.service.GameSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final GameSessionService gameSessionService;
    private final ObjectMapper objectMapper;
    
    // Track user states for card selection
    private final Map<Long, String> userStates = new ConcurrentHashMap<>();
    private final Map<Long, List<BingoCardsResponse>> userCardOptions = new ConcurrentHashMap<>();

    public TelegramBot(UserRepository userRepository, GameSessionService gameSessionService, ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.userRepository = userRepository;
        this.gameSessionService = gameSessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onUpdateReceived(Update update) {
        WebAppInfo webAppInfo = new WebAppInfo();
        webAppInfo.setUrl("https://definite-bird-briefly.ngrok-free.app ");

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            Long telegramId = update.getMessage().getFrom().getId();

            switch (messageText) {
                case "/start":
                    handleStartCommand(chatId, update.getMessage().getFrom().getFirstName());
                    break;
                case "/play":
                    handlePlayCommand(chatId, telegramId);
                    break;
                case "/join":
                    handleJoinCommand(chatId, telegramId);
                    break;
                case "/watch":
                    handleWatchCommand(chatId, telegramId);
                    break;
                case "/status":
                    handleStatusCommand(chatId, telegramId);
                    break;
                case "/bingo":
                    handleBingoCommand(chatId, telegramId);
                    break;
                case "/help":
                    handleHelpCommand(chatId);
                    break;
                case "🎮 Play Bingo":
                    handlePlayBingoButton(chatId, telegramId);
                    break;
                case "👁️ Watch Game":
                    handleWatchCommand(chatId, telegramId);
                    break;
                case "📊 Game Status":
                    handleStatusCommand(chatId, telegramId);
                    break;
                case "❓ Help":
                    handleHelpCommand(chatId);
                    break;
                default:
                    handleCardSelection(chatId, telegramId, messageText);
                    break;
            }
        }

        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }

        if (update.hasMessage() && update.getMessage().hasContact()) {
            handleContactRegistration(update);
        }
    }

    private void handleStartCommand(Long chatId, String firstName) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        KeyboardButton contactButton = new KeyboardButton("Share phone number");
        contactButton.setRequestContact(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(contactButton);

        KeyboardRow row2 = new KeyboardRow();
        KeyboardButton playButton = new KeyboardButton("🎮 Play Bingo");
        KeyboardButton watchButton = new KeyboardButton("👁️ Watch Game");
        row2.add(playButton);
        row2.add(watchButton);

        KeyboardRow row3 = new KeyboardRow();
        KeyboardButton statusButton = new KeyboardButton("📊 Game Status");
        KeyboardButton helpButton = new KeyboardButton("❓ Help");
        row3.add(statusButton);
        row3.add(helpButton);

        keyboardMarkup.setKeyboard(List.of(row1, row2, row3));

        sendMessage(chatId.toString(),
                "🎯 Welcome to Habesha Bingo, " + firstName + "!\n\n" +
                "🎮 **Play Bingo** - Open the web app to play\n" +
                "👁️ **Watch Game** - Spectate without playing\n" +
                "📊 **Game Status** - Check current game status\n" +
                "❓ **Help** - Show available commands\n\n" +
                "Please share your phone number to register:",
                keyboardMarkup);
    }

    private void handlePlayCommand(Long chatId, Long telegramId) {
        // Use the same web app opening logic as the button
        handlePlayBingoButton(chatId, telegramId);
    }

    private void handleJoinCommand(Long chatId, Long telegramId) {
        try {
            Optional<User> user = userRepository.findByTelegramId(telegramId);
            if (user.isEmpty()) {
                sendMessage(chatId.toString(), "❌ Please register first by sharing your phone number.");
                return;
            }

            GameSessionResponse gameStatus = gameSessionService.getActiveGameSession();
            if (gameStatus != null && gameStatus.getPhase().equals("cardSelection")) {
                showCardSelectionOptions(chatId, telegramId);
            } else {
                sendMessage(chatId.toString(), "❌ No active game in card selection phase. Use /play to start a new game.");
            }
        } catch (Exception e) {
            sendMessage(chatId.toString(), "❌ Error: " + e.getMessage());
        }
    }

    private void handleWatchCommand(Long chatId, Long telegramId) {
        try {
            GameSessionResponse gameStatus = gameSessionService.getActiveGameSession();
            if (gameStatus != null) {
                sendGameStatus(chatId, gameStatus, true);
                // Start polling for updates
                startGamePolling(chatId, gameStatus.getSessionCode());
            } else {
                sendMessage(chatId.toString(), "❌ No active game to watch. Use /play to start a new game.");
            }
        } catch (Exception e) {
            sendMessage(chatId.toString(), "❌ Error: " + e.getMessage());
        }
    }

    private void handleStatusCommand(Long chatId, Long telegramId) {
        try {
            GameSessionResponse gameStatus = gameSessionService.getActiveGameSession();
            if (gameStatus != null) {
                sendGameStatus(chatId, gameStatus, false);
            } else {
                sendMessage(chatId.toString(), "📊 No active game running. Use /play to start a new game.");
            }
        } catch (Exception e) {
            sendMessage(chatId.toString(), "❌ Error: " + e.getMessage());
        }
    }

    private void handleBingoCommand(Long chatId, Long telegramId) {
        try {
            Optional<User> user = userRepository.findByTelegramId(telegramId);
            if (user.isEmpty()) {
                sendMessage(chatId.toString(), "❌ Please register first by sharing your phone number.");
                return;
            }

            // Call BINGO
            BingoCallRequest bingoRequest = BingoCallRequest.builder()
                    .telegramId(telegramId)
                    .build();

            GameSessionResponse result = gameSessionService.callBingo(bingoRequest);
            
            if (result.getWinner() != null) {
                broadcastToAllPlayers("🎉 **BINGO!** 🎉\n\n" + result.getWinner() + " has won the game!");
            } else {
                sendMessage(chatId.toString(), "❌ False BINGO! You don't have a winning pattern.");
            }
        } catch (Exception e) {
            sendMessage(chatId.toString(), "❌ Error: " + e.getMessage());
        }
    }

    private void handlePlayBingoButton(Long chatId, Long telegramId) {
        try {
            // Check if user is registered
            Optional<User> user = userRepository.findByTelegramId(telegramId);
            if (user.isEmpty()) {
                sendMessage(chatId.toString(), "❌ Please register first by sharing your phone number.");
                return;
            }

            // Create web app button
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            InlineKeyboardButton webAppButton = new InlineKeyboardButton();
            webAppButton.setText("🎮 Open Bingo Game");
            webAppButton.setWebApp(new WebAppInfo("https://definite-bird-briefly.ngrok-free.app/"));

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(webAppButton);
            inlineKeyboardMarkup.setKeyboard(List.of(row));

            sendMessage(chatId.toString(), 
                "🎯 **Opening Habesha Bingo Game!**\n\n" +
                "🎮 Click the button below to open the game in your browser:\n\n" +
                "📱 The game will open in a web app where you can:\n" +
                "• Select your Bingo card\n" +
                "• Play the game in real-time\n" +
                "• Watch numbers being called\n" +
                "• Call BINGO when you win!",
                inlineKeyboardMarkup);
        } catch (Exception e) {
            sendMessage(chatId.toString(), "❌ Error opening game: " + e.getMessage());
        }
    }

    private void handleHelpCommand(Long chatId) {
        String helpText = """
            🎯 **Habesha Bingo - Available Commands**
            
            🎮 **/play** - Open the web app to play Bingo
            👁️ **/watch** - Watch current game as spectator
            📊 **/status** - Check current game status
            🎉 **/bingo** - Call BINGO when you have a winning pattern
            ❓ **/help** - Show this help message
            
            **How to Play:**
            1. Register with your phone number
            2. Press "🎮 Play Bingo" or use /play
            3. The web app will open in your browser
            4. Select your Bingo card in the web app
            5. Play the game in real-time
            6. Call BINGO when you have 5 in a row
            
            **Game Rules:**
            • Numbers 1-75 are used
            • Get 5 numbers in a row (horizontal, vertical, or diagonal)
            • Center square is a free space
            • False BINGO calls result in losing
            
            **Web App Features:**
            • Real-time game updates
            • Interactive Bingo card
            • Automatic number calling
            • Winner verification
            """;
        
        sendMessage(chatId.toString(), helpText);
    }

    private void handleCardSelection(Long chatId, Long telegramId, String messageText) {
        String state = userStates.get(telegramId);
        if (state != null && state.startsWith("SELECTING_CARD_")) {
            try {
                int cardIndex = Integer.parseInt(messageText) - 1;
                List<BingoCardsResponse> cards = userCardOptions.get(telegramId);
                
                if (cards != null && cardIndex >= 0 && cardIndex < cards.size()) {
                    BingoCardsResponse selectedCard = cards.get(cardIndex);
                    
                    // Join game with selected card
                    JoinGameRequest joinRequest = JoinGameRequest.builder()
                            .telegramId(telegramId)
                            .selectedCardCode(selectedCard.getCardCode())
                            .cardNumbers(selectedCard.getCardNumbers())
                            .build();
                    
                    GameSessionResponse gameSession = gameSessionService.joinGame(joinRequest);
                    
                    // Clear user state
                    userStates.remove(telegramId);
                    userCardOptions.remove(telegramId);
                    
                    // Send confirmation
                    sendMessage(chatId.toString(), 
                            "✅ You've joined the game with Card #" + (cardIndex + 1) + "!\n\n" +
                            "🎮 **Game Status:**\n" +
                            "• Phase: " + gameSession.getPhase() + "\n" +
                            "• Players: " + gameSession.getPlayerCount() + "\n" +
                            "• Called Numbers: " + gameSession.getCalledNumbers().size() + "\n\n" +
                            "The game will start automatically when the countdown ends!");
                    
                    // Broadcast to all players
                    broadcastToAllPlayers("🎮 **New Player Joined!**\n" +
                            "Player: " + getUserName(telegramId) + "\n" +
                            "Total Players: " + gameSession.getPlayerCount());
                    
                } else {
                    sendMessage(chatId.toString(), "❌ Invalid card number. Please select a number between 1 and " + cards.size());
                }
            } catch (NumberFormatException e) {
                sendMessage(chatId.toString(), "❌ Please enter a valid number.");
            } catch (Exception e) {
                sendMessage(chatId.toString(), "❌ Error joining game: " + e.getMessage());
            }
        }
    }

    private void handleCallbackQuery(org.telegram.telegrambots.meta.api.objects.CallbackQuery callbackQuery) {
        // Handle inline keyboard callbacks if needed
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Long telegramId = callbackQuery.getFrom().getId();
        
        // Add callback handling logic here
    }

    private void showCardSelectionOptions(Long chatId, Long telegramId) {
        try {
            // Generate 5 random card options for the user
            List<BingoCardsResponse> allCards = gameSessionService.generateBingoCard();
            List<BingoCardsResponse> userCards = new ArrayList<>();
            
            Random random = new Random();
            for (int i = 0; i < 5; i++) {
                userCards.add(allCards.get(random.nextInt(allCards.size())));
            }
            
            userCardOptions.put(telegramId, userCards);
            userStates.put(telegramId, "SELECTING_CARD_" + System.currentTimeMillis());
            
            StringBuilder message = new StringBuilder();
            message.append("🎯 **Select Your Bingo Card**\n\n");
            message.append("Choose one of the following cards:\n\n");
            
            for (int i = 0; i < userCards.size(); i++) {
                BingoCardsResponse card = userCards.get(i);
                message.append("**Card ").append(i + 1).append(":**\n");
                message.append(formatBingoCard(card.getCardNumbers()));
                message.append("\n");
            }
            
            message.append("Reply with the card number (1-5) to select your card:");
            
            sendMessage(chatId.toString(), message.toString());
            
        } catch (Exception e) {
            sendMessage(chatId.toString(), "❌ Error generating cards: " + e.getMessage());
        }
    }

    private String formatBingoCard(List<List<Integer>> cardNumbers) {
        StringBuilder sb = new StringBuilder();
        String[] headers = {"B", "I", "N", "G", "O"};
        
        // Add headers
        sb.append("  ");
        for (String header : headers) {
            sb.append(header).append("  ");
        }
        sb.append("\n");
        
        // Add rows
        for (int i = 0; i < cardNumbers.size(); i++) {
            sb.append(i + 1).append(" ");
            for (int j = 0; j < cardNumbers.get(i).size(); j++) {
                int num = cardNumbers.get(i).get(j);
                if (num == 0) {
                    sb.append("★  "); // Free space
                } else {
                    sb.append(String.format("%2d ", num));
                }
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }

    private void startNewGame(Long chatId, Long telegramId) {
        try {
            // Generate cards and start game
            List<BingoCardsResponse> allCards = gameSessionService.generateBingoCard();
            
            // Create initial game session
            GameSessionResponse gameSession = gameSessionService.getActiveGameSession();
            
            sendMessage(chatId.toString(), 
                    "🎮 **New Bingo Game Started!**\n\n" +
                    "Use /join to select your card and join the game.\n" +
                    "Use /watch to spectate the game.\n\n" +
                    "Game will start in 30 seconds!");
            
            // Broadcast to all registered users
            broadcastToAllUsers("🎮 **New Bingo Game Started!**\n\n" +
                    "Use /join to play or /watch to spectate!");
            
        } catch (Exception e) {
            sendMessage(chatId.toString(), "❌ Error starting game: " + e.getMessage());
        }
    }

    private void sendGameStatus(Long chatId, GameSessionResponse gameStatus, boolean isWatching) {
        StringBuilder status = new StringBuilder();
        
        if (isWatching) {
            status.append("👁️ **Watching Bingo Game**\n\n");
        } else {
            status.append("📊 **Game Status**\n\n");
        }
        
        status.append("🎯 **Phase:** ").append(gameStatus.getPhase()).append("\n");
        status.append("👥 **Players:** ").append(gameStatus.getPlayerCount()).append("\n");
        status.append("🔢 **Called Numbers:** ").append(gameStatus.getCalledNumbers().size()).append("/75\n");
        
        if (gameStatus.getCurrentCall() != null) {
            status.append("📢 **Last Called:** ").append(gameStatus.getCurrentCall()).append("\n");
        }
        
        if (gameStatus.getWinner() != null) {
            status.append("🏆 **Winner:** ").append(gameStatus.getWinner()).append("\n");
        }
        
        if (gameStatus.getCalledNumbers() != null && !gameStatus.getCalledNumbers().isEmpty()) {
            status.append("\n📋 **Called Numbers:**\n");
            List<Integer> calledNumbers = new ArrayList<>(gameStatus.getCalledNumbers());
            Collections.sort(calledNumbers);
            
            for (int i = 0; i < calledNumbers.size(); i += 10) {
                int end = Math.min(i + 10, calledNumbers.size());
                status.append(calledNumbers.subList(i, end)).append("\n");
            }
        }
        
        sendMessage(chatId.toString(), status.toString());
    }

    private void startGamePolling(Long chatId, String sessionCode) {
        // Start a background thread to poll for game updates
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(5000); // Poll every 5 seconds
                    
                    GameSessionResponse gameStatus = gameSessionService.getGameSession(sessionCode);
                    if (gameStatus != null) {
                        // Send updates to spectator
                        sendGameStatus(chatId, gameStatus, true);
                        
                        // Check if game ended
                        if (gameStatus.getWinner() != null) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                // Stop polling on error
            }
        }).start();
    }

    private void broadcastToAllPlayers(String message) {
        try {
            // Get all registered users and send them the message
            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                try {
                    sendMessage(user.getTelegramId().toString(), message);
                    Thread.sleep(100); // Small delay to avoid rate limiting
                } catch (Exception e) {
                    // Continue with other users if one fails
                }
            }
        } catch (Exception e) {
            // Log error but don't stop the game
        }
    }

    private void broadcastToAllUsers(String message) {
        try {
            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                try {
                    sendMessage(user.getTelegramId().toString(), message);
                    Thread.sleep(100);
                } catch (Exception e) {
                    // Continue with other users
                }
            }
        } catch (Exception e) {
            // Log error
        }
    }

    private String getUserName(Long telegramId) {
        try {
            Optional<User> user = userRepository.findByTelegramId(telegramId);
            return user.map(User::getFirstName).orElse("Unknown Player");
        } catch (Exception e) {
            return "Unknown Player";
        }
    }

    private void handleContactRegistration(Update update) {
        Long telegramId = update.getMessage().getFrom().getId();
        String phoneNumber = update.getMessage().getContact().getPhoneNumber();
        String userName = update.getMessage().getFrom().getUserName();
        String firstName = update.getMessage().getFrom().getFirstName();
        String lastName = update.getMessage().getFrom().getLastName();

        UserDto userDto = UserDto.builder()
                .phoneNumber(phoneNumber)
                .userName(userName)
                .firstName(firstName)
                .telegramId(telegramId)
                .lastName(lastName)
                .build();

        String url = "http://localhost:8080/users/register";
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, userDto, String.class);
            sendMessage(String.valueOf(update.getMessage().getChatId()), 
                    "✅ " + response.getBody() + "\n\n" +
                    "🎮 You can now press '🎮 Play Bingo' to open the web app and start playing!");
        } catch (HttpClientErrorException e) {
            sendMessage(String.valueOf(update.getMessage().getChatId()), e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            sendMessage(String.valueOf(update.getMessage().getChatId()), "❌ Registration failed. Please try again.");
        }
    }

    @Override
    public String getBotUsername() {
        return "HabeshBingoBot";
    }

    @Override
    public String getBotToken() {
        return "8292455357:AAF7CwlHh2pzdtVyFV7JO72JoB5zh8yAEBo";
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    public void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableMarkdown(true);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    public void sendMessage(String chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(inlineKeyboardMarkup);
        message.enableMarkdown(true);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    public void sendMessage(String chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        message.enableMarkdown(true);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
}
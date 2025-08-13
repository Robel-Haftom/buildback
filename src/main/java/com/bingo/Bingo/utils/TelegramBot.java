package com.bingo.Bingo.utils;

import com.bingo.Bingo.dto.request.UserDto;
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

import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final RestTemplate restTemplate;

    public TelegramBot() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                Long chatId = update.getMessage().getChatId();

                if (messageText.equals("/start")) {

                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    keyboardMarkup.setSelective(true);
                    keyboardMarkup.setResizeKeyboard(true);
                    keyboardMarkup.setOneTimeKeyboard(true);

                    KeyboardButton contactButton = new KeyboardButton("Share phone number");
                    contactButton.setRequestContact(true);

                    KeyboardRow row = new KeyboardRow();
                    row.add(contactButton);

                    keyboardMarkup.setKeyboard(List.of(row));

                    sendMessage(chatId.toString(),
                            "Welcome to Habesha Bingo, " + update.getMessage().getFrom().getFirstName() + ". Please share your phone number:",
                            keyboardMarkup);
                }
                if (messageText.equals("/play")) {
                    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText("Play Bingo");

                    WebAppInfo webAppInfo = new WebAppInfo();
                    webAppInfo.setUrl("https://80c45ea8f34a.ngrok-free.app");
                    button.setWebApp(webAppInfo);

                    inlineKeyboardMarkup.setKeyboard(List.of(List.of(button)));

                    String url = "http://localhost:8080/users/" + update.getMessage().getFrom().getId();
                    try {
                        ResponseEntity<UserDto> response = restTemplate.getForEntity(url, UserDto.class);
                        sendMessage(chatId.toString(), "Good Luck. 🤞", inlineKeyboardMarkup);
                    } catch (HttpClientErrorException.NotFound e) {
                        sendMessage(chatId.toString(), "Please register first by sharing your phone number.");
                    } catch (Exception e) {
                        System.err.println("Error fetching user: " + e.getMessage());
                        sendMessage(chatId.toString(), "An error occurred. Please try again later.");
                    }
                }
            }

        if (update.hasMessage() && update.getMessage().hasContact()) {
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
                sendMessage(String.valueOf(update.getMessage().getChatId()), response.getBody());
            } catch (HttpClientErrorException e) {
                sendMessage(String.valueOf(update.getMessage().getChatId()), e.getResponseBodyAsString());
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }
    }
    @Override
    public String getBotUsername() {
        return "HabeshBingoBot"; // Replace with your bot's username
    }

    @Override
    public String getBotToken() {
        return "8292455357:AAF7CwlHh2pzdtVyFV7JO72JoB5zh8yAEBo"; // Replace with your bot's token
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    public void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
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
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
}
package com.bingo.Bingo.controller;

import com.bingo.Bingo.dto.request.UserDto;
import com.bingo.Bingo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserDto userDto) {
        UserDto registeredUser = userService.registerUser(userDto);
        return ResponseEntity.ok(String.format("Registered successfully with name: %s and phone number: %s. click /play to start the game.",
                registeredUser.getFirstName(), registeredUser.getPhoneNumber()));
    }

    @GetMapping("/{telegramId}")
    public ResponseEntity<UserDto> getUserByTelegramId(@PathVariable Long telegramId) {
        UserDto user = userService.getUserByTelegramId(telegramId);
        return ResponseEntity.ok(user);
    }

}

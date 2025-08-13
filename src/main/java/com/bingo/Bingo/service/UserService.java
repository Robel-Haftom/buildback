package com.bingo.Bingo.service;

import com.bingo.Bingo.dto.request.UserDto;

public interface UserService {
    UserDto registerUser(UserDto userDto);

    UserDto getUserByTelegramId(Long telegramId);
}

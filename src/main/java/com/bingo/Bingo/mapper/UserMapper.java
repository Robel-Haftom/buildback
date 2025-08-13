package com.bingo.Bingo.mapper;

import com.bingo.Bingo.dto.request.UserDto;
import com.bingo.Bingo.entity.User;

public class UserMapper {

    public static UserDto toDto(User user) {
        return UserDto.builder()
                .telegramId(user.getTelegramId())
                .phoneNumber(user.getPhoneNumber())
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    public static User toEntity(UserDto userDto) {
        return User.builder()
                .telegramId(userDto.getTelegramId())
                .phoneNumber(userDto.getPhoneNumber())
                .userName(userDto.getUserName())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .build();
    }

}

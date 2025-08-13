package com.bingo.Bingo.service.Impl;

import com.bingo.Bingo.dto.request.UserDto;
import com.bingo.Bingo.entity.User;
import com.bingo.Bingo.exception.ResourceExistsException;
import com.bingo.Bingo.exception.ResourceNotFound;
import com.bingo.Bingo.mapper.UserMapper;
import com.bingo.Bingo.repository.UserRepository;
import com.bingo.Bingo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;


    @Override
    public UserDto registerUser(UserDto userDto) {
        userRepository.findByTelegramId(userDto.getTelegramId())
                .ifPresent(user -> {
                    throw new ResourceExistsException("Already Registered, Click /play to start the Game");
                });
        User newUser = UserMapper.toEntity(userDto);

        userRepository.save(newUser);

        return UserMapper.toDto(newUser);
    }

    @Override
    public UserDto getUserByTelegramId(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new ResourceNotFound("User not found with Telegram ID: " + telegramId));
        return UserMapper.toDto(user);
    }
}

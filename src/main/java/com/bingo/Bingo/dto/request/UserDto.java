package com.bingo.Bingo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long telegramId;
    private String phoneNumber;
    private String userName;
    private String firstName;
    private String lastName;
}

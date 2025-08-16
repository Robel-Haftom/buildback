package com.bingo.Bingo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerInfo {
    private Long telegramId;
    private String firstName;
    private String lastName;
    private String userName;
    private Integer selectedCardCode;
    private Boolean isWinner;
}

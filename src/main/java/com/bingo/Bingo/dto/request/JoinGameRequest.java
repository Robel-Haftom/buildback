package com.bingo.Bingo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinGameRequest {
    private Long telegramId;
    private Integer selectedCardCode;
    private List<List<Integer>> cardNumbers;
}

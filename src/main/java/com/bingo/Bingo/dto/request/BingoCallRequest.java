package com.bingo.Bingo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BingoCallRequest {
    private Long telegramId;
    private Integer selectedCardCode;
}

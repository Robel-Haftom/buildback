package com.bingo.Bingo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BingoCardsResponse {
    private int cardCode;
    private List<List<Integer>> cardNumbers;
}

package com.bingo.Bingo.service;

import com.bingo.Bingo.dto.response.BingoCardsResponse;

import java.util.List;

public interface GameService {

    List<BingoCardsResponse> generateBingoCard();
}

package com.bingo.Bingo.service.Impl;

import com.bingo.Bingo.dto.response.BingoCardsResponse;
import com.bingo.Bingo.service.GameService;
import com.bingo.Bingo.utils.GameFunctions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GameServiceImpl implements GameService {


    @Override
    public List<BingoCardsResponse> generateBingoCard() {
        List<BingoCardsResponse> allBingoCards = new ArrayList<>();
        for(int i=0; i<400; i++){
            BingoCardsResponse bingoCardsResponse = BingoCardsResponse.builder()
                    .cardCode(i)
                    .cardNumbers(GameFunctions.generateBingoCard())
                    .build();
            allBingoCards.add(bingoCardsResponse);
        }

        return allBingoCards;
    }
}

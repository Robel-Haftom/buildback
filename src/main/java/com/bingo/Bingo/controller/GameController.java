package com.bingo.Bingo.controller;

import com.bingo.Bingo.dto.response.BingoCardsResponse;
import com.bingo.Bingo.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/start")
    public ResponseEntity<List<BingoCardsResponse>> startGame() {
        return ResponseEntity.ok(gameService.generateBingoCard());
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello, welcome to the Bingo game!";
    }
}

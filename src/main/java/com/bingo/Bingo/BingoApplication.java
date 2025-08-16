package com.bingo.Bingo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BingoApplication {

	public static void main(String[] args) {
		SpringApplication.run(BingoApplication.class, args);
	}

}

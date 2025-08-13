package com.bingo.Bingo.repository;

import com.bingo.Bingo.entity.BingoCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BingoCardRepository extends JpaRepository<BingoCard, Long> {

}

package com.bingo.Bingo.repository;

import com.bingo.Bingo.entity.PlayerGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface PlayerGameRepository extends JpaRepository<PlayerGame, Long> {

}

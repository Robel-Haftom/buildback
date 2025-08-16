package com.bingo.Bingo.repository;

import com.bingo.Bingo.entity.PlayerGameSession;
import com.bingo.Bingo.entity.User;
import com.bingo.Bingo.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerGameSessionRepository extends JpaRepository<PlayerGameSession, Long> {
    Optional<PlayerGameSession> findByUserAndGameSession(User user, GameSession gameSession);
    List<PlayerGameSession> findByGameSessionId(Long gameSessionId);
}

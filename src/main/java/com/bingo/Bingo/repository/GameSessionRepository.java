package com.bingo.Bingo.repository;

import com.bingo.Bingo.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    Optional<GameSession> findBySessionCode(String sessionCode);
    
    @Query("SELECT gs FROM GameSession gs LEFT JOIN FETCH gs.playerSessions ps LEFT JOIN FETCH ps.user WHERE gs.sessionCode = :sessionCode")
    Optional<GameSession> findBySessionCodeWithPlayers(@Param("sessionCode") String sessionCode);
}

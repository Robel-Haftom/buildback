package com.bingo.Bingo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "player_game_sessions")
public class PlayerGameSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;
    
    @Column(name = "selected_card_code", nullable = false)
    private Integer selectedCardCode;
    
    @Column(name = "card_numbers_json", columnDefinition = "TEXT")
    private String cardNumbersJson;
    
    @Column(name = "is_winner")
    private Boolean isWinner;
    
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    private void onCreate() {
        this.joinedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isWinner = false;
    }
    
    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "PlayerGameSession{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", gameSessionId=" + (gameSession != null ? gameSession.getId() : null) +
                ", selectedCardCode=" + selectedCardCode +
                ", cardNumbersJson='" + cardNumbersJson + '\'' +
                ", isWinner=" + isWinner +
                ", joinedAt=" + joinedAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

package com.bingo.Bingo.entity;

import com.bingo.Bingo.common.enums.GameStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "game_sessions")
public class GameSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_code", unique = true, nullable = false)
    private String sessionCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GameStatus status;
    
    @Column(name = "phase", nullable = false)
    private String phase; // "cardSelection", "gameRoom"
    
    @Column(name = "countdown", nullable = false)
    private Integer countdown;
    
    @Column(name = "game_active", nullable = false)
    private Boolean gameActive;
    
    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PlayerGameSession> playerSessions = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "game_session_called_numbers", joinColumns = @JoinColumn(name = "game_session_id"))
    @Column(name = "called_number")
    private Set<Integer> calledNumbers = new HashSet<>();
    
    @Column(name = "current_call")
    private Integer currentCall;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_player")
    private PlayerGameSession winningPlayer;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = GameStatus.ONGOING;
        this.phase = "cardSelection";
        this.countdown = 30;
        this.gameActive = false;
    }
    
    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "GameSession{" +
                "id=" + id +
                ", sessionCode='" + sessionCode + '\'' +
                ", status=" + status +
                ", phase='" + phase + '\'' +
                ", countdown=" + countdown +
                ", gameActive=" + gameActive +
                ", playerSessions.size=" + (playerSessions != null ? playerSessions.size() : 0) +
                ", calledNumbers.size=" + (calledNumbers != null ? calledNumbers.size() : 0) +
                ", currentCall=" + currentCall +
                ", winningPlayerId=" + (winningPlayer != null ? winningPlayer.getId() : null) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

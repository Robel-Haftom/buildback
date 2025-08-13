package com.bingo.Bingo.entity;

import com.bingo.Bingo.common.enums.GameStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "games",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"game_id"}
                )
        }
)
public class Game {

    @SequenceGenerator(
            name = "game_sequence",
            sequenceName = "game_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "game_sequence"
    )
    @Column(name = "game_id", nullable = false, unique = true)
    @Id
    private Long id;

    @Column(name = "game_start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "game_status", nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private GameStatus gameStatus = GameStatus.ONGOING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_user")
    private User winningUser;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}



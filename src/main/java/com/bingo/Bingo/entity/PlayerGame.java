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
@Table(
        name = "player_games",
        uniqueConstraints = {
            @UniqueConstraint(
                columnNames = {"user_id", "game_id"}
            )
        }
)
public class PlayerGame {

    @SequenceGenerator(
            name = "player_game_sequence",
            sequenceName = "player_game_sequence",
            allocationSize = 1
    )
    @GeneratedValue
            (       strategy = GenerationType.SEQUENCE,
                    generator = "player_game_sequence")
    @Column(name = "player_game_id", nullable = false, unique = true)
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "bingo_card_id", nullable = false)
    private Long bingoCardId;  // Assuming this is the ID of the BingoCard associated with the player

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_winner", nullable = false)
    @Builder.Default
    private Boolean isWinner = false;  // Indicates if the player has won the game

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "PlayerGame{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", gameId=" + (game != null ? game.getId() : null) +
                ", bingoCardId=" + bingoCardId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isWinner=" + isWinner +
                ", isActive=" + isActive +
                '}';
    }
}

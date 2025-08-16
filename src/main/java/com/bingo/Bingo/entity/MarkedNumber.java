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
        name = "marked_numbers",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"marked_number_id"}
                )
        }
)
public class MarkedNumber {

    @SequenceGenerator(
            name = "marked_number_sequence",
            sequenceName = "marked_number_sequence",
            allocationSize = 1
    )
    @GeneratedValue
            (strategy = GenerationType.SEQUENCE, generator = "marked_number_sequence")
    @Column(name = "marked_number_id", nullable = false, unique = true)
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_game_id", nullable = false)
    private PlayerGame playerGame;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "called_number_id", nullable = false)
    private CalledNumber calledNumber;

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

    @Override
    public String toString() {
        return "MarkedNumber{" +
                "id=" + id +
                ", playerGameId=" + (playerGame != null ? playerGame.getId() : null) +
                ", calledNumberId=" + (calledNumber != null ? calledNumber.getId() : null) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

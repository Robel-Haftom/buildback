package com.bingo.Bingo.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "bingo_cards",
        uniqueConstraints = {
            @UniqueConstraint(
                columnNames = {"card_number"}
            )
        }
)
public class BingoCard {

    @SequenceGenerator(
            name = "bingo_card_sequence",
            sequenceName = "bingo_card_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "bingo_card_sequence"
    )
    @Column(name = "id", nullable = false, unique = true)
    @Id
    private Long id;

    @Column(name = "card_number", nullable = false, unique = true)
    private Integer cardNumber;

    @Transient
    private Integer[][] numbers;

    @Column(name = "numbers_json", nullable = false, columnDefinition = "TEXT")
    private String numbersJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() throws JsonProcessingException {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        ObjectMapper objectMapper = new ObjectMapper();
        this.numbersJson = objectMapper.writeValueAsString(numbers);
    }

    @PreUpdate
    private void onUpdate() throws JsonProcessingException {
        this.updatedAt = LocalDateTime.now();

        ObjectMapper objectMapper = new ObjectMapper();
        this.numbersJson = objectMapper.writeValueAsString(numbers);
    }

    @Override
    public String toString() {
        return "BingoCard{" +
                "id=" + id +
                ", cardNumber=" + cardNumber +
                ", numbers=" + (numbers != null ? "[" + numbers.length + "x" + (numbers.length > 0 ? numbers[0].length : 0) + "]" : "null") +
                ", numbersJson='" + numbersJson + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

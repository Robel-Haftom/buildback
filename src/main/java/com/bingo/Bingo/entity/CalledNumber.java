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
@Table( name = "called_numbers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"number_id"})
        }
)
public class CalledNumber {

    @SequenceGenerator(
            name = "called_number_sequence",
            sequenceName = "called_number_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = jakarta.persistence.GenerationType.SEQUENCE,
            generator = "called_number_sequence"
    )
    @Column(name = "number_id", nullable = false, unique = true)
    @Id
    private Long id;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "letter", nullable = false)
    private String letter;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "call_order", nullable = false, unique = true)
    private Integer callOrder;

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
        return "CalledNumber{" +
                "id=" + id +
                ", number=" + number +
                ", letter='" + letter + '\'' +
                ", createdAt=" + createdAt +
                ", callOrder=" + callOrder +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

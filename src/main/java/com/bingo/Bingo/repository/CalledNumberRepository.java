package com.bingo.Bingo.repository;

import com.bingo.Bingo.entity.CalledNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalledNumberRepository extends JpaRepository<CalledNumber, Long> {

}

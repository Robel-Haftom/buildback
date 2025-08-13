package com.bingo.Bingo.repository;

import com.bingo.Bingo.entity.MarkedNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MarkedNumberRepository extends JpaRepository<MarkedNumber, Long> {

}

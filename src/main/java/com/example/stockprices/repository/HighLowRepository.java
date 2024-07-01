package com.example.stockprices.repository;

import com.example.stockprices.model.prices.highlow.HighLowForPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HighLowRepository extends JpaRepository<HighLowForPeriod, Long> {
}

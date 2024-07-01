package com.example.stockprices.repository;

import com.example.stockprices.model.prices.StockWeeklyHistoricalPrices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockWeeklyHistoricalPricesRepository extends JpaRepository<StockWeeklyHistoricalPrices, Long> {
}

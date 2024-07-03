package stock.price.analytics.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.StockHistoricalPrices;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockHistoricalPricesRepository extends JpaRepository<StockHistoricalPrices, Long> {
    List<StockHistoricalPrices> findByTicker(String ticker);
    List<StockHistoricalPrices> findByTickerAndDateBetweenOrderByDateDesc(String ticker, LocalDate startDate, LocalDate endDate);

}
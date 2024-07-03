package stock.price.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.StockWeeklyHistoricalPrices;

@Repository
public interface StockWeeklyHistoricalPricesRepository extends JpaRepository<StockWeeklyHistoricalPrices, Long> {
}

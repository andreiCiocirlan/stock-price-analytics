package stock.price.analytics.repository.stocks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.stocks.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    @Query(value = """
            UPDATE stocks s
            SET delisted_date = (
              SELECT MAX(dp.date)
              FROM daily_prices dp
              WHERE dp.ticker = s.ticker
              HAVING MAX(dp.date) < CURRENT_DATE - INTERVAL '20 day'
            ), ipo_date = (
              SELECT MIN(dp.date)
              FROM daily_prices dp
              WHERE dp.ticker = s.ticker
            )
            """, nativeQuery = true)
    void updateIpoAndDelistedDates();
}

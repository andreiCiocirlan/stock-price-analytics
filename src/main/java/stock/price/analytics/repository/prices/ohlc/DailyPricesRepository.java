package stock.price.analytics.repository.prices.ohlc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface DailyPricesRepository extends JpaRepository<DailyPrice, Long> {

    long countByDateBefore(LocalDate date);
    long countByDateAfter(LocalDate date);
    long countByDateBetween(LocalDate startDate, LocalDate endDate);

    List<DailyPrice> findByTickerIn(List<String> tickerList);
    List<DailyPrice> findByDate(LocalDate date);
    List<DailyPrice> findByDateBefore(LocalDate date);
    List<DailyPrice> findByDateAfterOrderByDate(LocalDate date);
    List<DailyPrice> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query(value = """
            WITH latest_prices AS (
                SELECT *,
                       ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS row_num
                FROM daily_prices
                WHERE date >= CURRENT_DATE - interval '7 days'
            )
            SELECT id, open, high, low, close, ticker, date, performance
            FROM latest_prices
            WHERE row_num <= 2;
            """, nativeQuery = true)
    List<DailyPrice> findLatestTwoDailyPrices();

    List<DailyPrice> findByTickerAndDateLessThan(String ticker, LocalDate date);

    List<DailyPrice> findByTickerAndDate(String ticker, LocalDate date);

}
